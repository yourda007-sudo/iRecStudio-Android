package com.irecstudio.service

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.irecstudio.state.OverlayStateHolder
import java.io.File

class ScreenRecordService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var tempFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0

    private val tickRunnable = object : Runnable {
        override fun run() {
            seconds++
            OverlayStateHolder.updateElapsed(seconds)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_RECORDING") {
            stopAndSaveToGallery()
            return START_NOT_STICKY
        }

        startRecForeground()

        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("DATA_INTENT", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("DATA_INTENT")
        }

        if (resultCode != Activity.RESULT_OK || dataIntent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mp = mpm.getMediaProjection(resultCode, dataIntent)
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopAndSaveToGallery()
            }
        }, handler)
        projection = mp

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val width = (metrics.widthPixels / 2) * 2
        val height = (metrics.heightPixels / 2) * 2
        val dpi = metrics.densityDpi

        val outFile = File(cacheDir, "irec_${System.currentTimeMillis()}.mp4")
        tempFile = outFile

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        rec.apply {
            if (OverlayStateHolder.state.value.micEnabled) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoFrameRate(60)
            setVideoEncodingBitRate(8_000_000)
            if (OverlayStateHolder.state.value.micEnabled) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128_000)
            }
            setOutputFile(outFile.absolutePath)
            prepare()
        }
        recorder = rec

        virtualDisplay = mp.createVirtualDisplay(
            "iRecDisplay",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            rec.surface,
            null,
            null
        )

        rec.start()
        seconds = 0
        OverlayStateHolder.setRecording(true, 0)
        OverlayStateHolder.onStopRecordingRequested = { stopAndSaveToGallery() }
        handler.postDelayed(tickRunnable, 1000)
        return START_STICKY
    }

    private fun stopAndSaveToGallery() {
        handler.removeCallbacks(tickRunnable)
        OverlayStateHolder.onStopRecordingRequested = null
        OverlayStateHolder.setRecording(false, 0)

        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        projection?.stop()
        projection = null

        val file = tempFile
        tempFile = null
        if (file != null && file.exists() && file.length() > 0) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "iRec_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/iRecStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
            file.delete()
        }
        stopSelf()
    }

    private fun startRecForeground() {
        val chId = "irec_record"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(chId, "iRec Screen Recorder", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = NotificationCompat.Builder(this, chId)
            .setContentTitle("Recording Screen...")
            .setSmallIcon(android.R.drawable.presence_video_busy)
            .build()
        startForeground(102, notif)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}