package com.irecstudio

import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irecstudio.service.OverlayStatusBarService
import com.irecstudio.service.ScreenRecordService
import com.irecstudio.state.OverlayStateHolder

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    private val projLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK && res.data != null) {
            val svc = Intent(this, ScreenRecordService::class.java).apply {
                putExtra("RESULT_CODE", res.resultCode)
                putExtra("DATA_INTENT", res.data)
            }
            startForegroundService(svc)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(perms.toTypedArray())

        setContent {
            val uiState by OverlayStateHolder.state.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090A0F))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("iRec Studio", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dynamic Island", color = Color.White)
                    Switch(checked = uiState.showIsland, onCheckedChange = { OverlayStateHolder.toggleIsland() })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("পূর্ণ স্ট্যাটাস বার", color = Color.White)
                    Switch(checked = uiState.showStatusBar, onCheckedChange = { OverlayStateHolder.toggleStatusBar() })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("মাইক্রোফোন", color = Color.White)
                    Switch(checked = uiState.micEnabled, onCheckedChange = { OverlayStateHolder.toggleMic() })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ব্যাটারি %", color = Color.White)
                    Switch(checked = uiState.showBatteryPercent, onCheckedChange = { OverlayStateHolder.toggleBatteryPercent() })
                }

                Button(
                    onClick = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        } else if (uiState.overlayRunning) {
                            stopService(Intent(this@MainActivity, OverlayStatusBarService::class.java))
                        } else {
                            startForegroundService(Intent(this@MainActivity, OverlayStatusBarService::class.java))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (uiState.overlayRunning) "স্ট্যাটাস বার ওভারলে বন্ধ করুন" else "স্ট্যাটাস বার ওভারলে চালু করুন")
                }

                OutlinedButton(
                    onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("ব্রাউজার ডিটেকশন (Accessibility) অন করুন", color = Color.White)
                }

                Button(
                    onClick = {
                        if (uiState.isRecording) {
                            startService(Intent(this@MainActivity, ScreenRecordService::class.java).apply {
                                action = "STOP_RECORDING"
                            })
                        } else {
                            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            projLauncher.launch(mpm.createScreenCaptureIntent())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRecording) Color(0xFFFF3B30) else Color(0xFF30D158)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (uiState.isRecording) "রেকর্ডিং বন্ধ করুন" else "স্ক্রিন রেকর্ডিং শুরু করুন",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}