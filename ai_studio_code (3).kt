package com.irecstudio.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OverlayUiState(
    val overlayRunning: Boolean = false,
    val showIsland: Boolean = true,
    val showStatusBar: Boolean = true,
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val micEnabled: Boolean = true,
    val isBrowserActive: Boolean = false,
    val wifiOn: Boolean = true,
    val batteryLevel: Int = 85,
    val showBatteryPercent: Boolean = false,
    val expanded: Boolean = false
)

object OverlayStateHolder {
    private val _state = MutableStateFlow(OverlayUiState())
    val state: StateFlow<OverlayUiState> = _state.asStateFlow()

    var onStopRecordingRequested: (() -> Unit)? = null

    fun setOverlayRunning(running: Boolean) = _state.update { it.copy(overlayRunning = running) }
    fun toggleIsland() = _state.update { it.copy(showIsland = !it.showIsland) }
    fun toggleStatusBar() = _state.update { it.copy(showStatusBar = !it.showStatusBar) }
    fun toggleMic() = _state.update { it.copy(micEnabled = !it.micEnabled) }
    fun toggleWifi() = _state.update { it.copy(wifiOn = !it.wifiOn) }
    fun toggleBatteryPercent() = _state.update { it.copy(showBatteryPercent = !it.showBatteryPercent) }
    fun toggleExpanded() = _state.update { it.copy(expanded = !it.expanded) }
    fun setBrowserActive(active: Boolean) = _state.update { it.copy(isBrowserActive = active) }
    fun setRecording(recording: Boolean, elapsed: Int = 0) = _state.update {
        it.copy(isRecording = recording, elapsedSeconds = elapsed)
    }
    fun updateElapsed(seconds: Int) = _state.update { it.copy(elapsedSeconds = seconds) }
    fun updateBattery(level: Int) = _state.update { it.copy(batteryLevel = level.coerceIn(1, 100)) }
}