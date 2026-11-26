package com.irecstudio.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.irecstudio.state.OverlayStateHolder
import com.irecstudio.ui.DynamicStatusOverlay

class OverlayStatusBarService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        startOverlayForeground()

        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (pct in 1..100) OverlayStateHolder.updateBattery(pct)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayStatusBarService)
            setViewTreeSavedStateRegistryOwner(this@OverlayStatusBarService)
            setContent {
                val uiState by OverlayStateHolder.state.collectAsState()
                DynamicStatusOverlay(state = uiState)
            }
        }
        overlayView = view
        windowManager.addView(view, params)
        OverlayStateHolder.setOverlayRunning(true)
    }

    private fun startOverlayForeground() {
        val chId = "irec_overlay"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(chId, "iRec Status Overlay", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = NotificationCompat.Builder(this, chId)
            .setContentTitle("iRec Studio Overlay Active")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .build()
        startForeground(101, notif)
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        OverlayStateHolder.setOverlayRunning(false)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}