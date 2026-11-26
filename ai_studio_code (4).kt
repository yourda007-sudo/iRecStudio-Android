package com.irecstudio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irecstudio.state.OverlayStateHolder
import com.irecstudio.state.OverlayUiState
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun DynamicStatusOverlay(state: OverlayUiState) {
    if (!state.showIsland && !state.showStatusBar && !state.isRecording) return

    var clockText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("h:mm", Locale.US)
        while (true) {
            clockText = fmt.format(Date())
            delay(10_000)
        }
    }

    val micOutside = state.isRecording && state.isBrowserActive
    val statusBg by animateColorAsState(
        targetValue = if (micOutside) Color(0xFFF2F3F6) else Color(0xFF0A0C12),
        label = "statusBg"
    )
    val fgColor = if (micOutside) Color(0xFF0F172A) else Color.White

    val timerText = remember(state.elapsedSeconds) {
        val m = state.elapsedSeconds / 60
        val s = state.elapsedSeconds % 60
        String.format(Locale.US, "%02d:%02d", m, s)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.showStatusBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // বাম পাশে ঘড়ি / টাইমার
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { OverlayStateHolder.toggleExpanded() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!state.showIsland && state.isRecording) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                        )
                    }
                    Text(
                        text = if (state.isRecording) timerText else clockText,
                        color = fgColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // মাঝখানে Dynamic Island (যদি চালু থাকে)
                if (state.showIsland) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        IslandPill(state = state, micOutside = micOutside)
                    }
                }

                // ডান পাশে Wi-Fi, মাইক ডট ও ব্যাটারি
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!state.showIsland && state.micEnabled) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9F0A))
                        )
                    }
                    if (state.wifiOn) {
                        Text(text = "Wi-Fi", color = fgColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .border(1.2.dp, fgColor.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (state.showBatteryPercent) "${state.batteryLevel}%" else "  ",
                            color = fgColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (state.showIsland) {
            Box(modifier = Modifier.padding(top = 6.dp)) {
                IslandPill(state = state, micOutside = micOutside)
            }
        } else if (state.isRecording) {
            // দুটোই বন্ধ থাকলে মিনিমাল রেকর্ড ব্যাজ
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(CircleShape)
                    .background(Color(0xE612141C))
                    .clickable { OverlayStateHolder.toggleExpanded() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
                Text(text = timerText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Dynamic Island-এ চাপ দিলে Expanded Live Activity
        AnimatedVisibility(visible = state.expanded) {
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (state.isRecording) "REC $timerText" else "iRec Ready",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (state.micEnabled) Color(0xFFFF9F0A) else Color(0xFF2C2F3A))
                        .clickable { OverlayStateHolder.toggleMic() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (state.micEnabled) "Mic On" else "Mic Off",
                        color = if (state.micEnabled) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (state.isRecording) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFF3B30))
                            .clickable {
                                OverlayStateHolder.toggleExpanded()
                                OverlayStateHolder.onStopRecordingRequested?.invoke()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandPill(state: OverlayUiState, micOutside: Boolean) {
    val dotOffset by animateDpAsState(
        targetValue = if (micOutside) 14.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "micDot"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .width(126.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .clickable { OverlayStateHolder.toggleExpanded() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
            )
            if (state.micEnabled && !micOutside) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9F0A))
                )
            }
        }
        if (state.micEnabled && micOutside) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .offset(x = dotOffset - 14.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9F0A))
                    .border(1.5.dp, Color.Black, CircleShape)
            )
        }
    }
}