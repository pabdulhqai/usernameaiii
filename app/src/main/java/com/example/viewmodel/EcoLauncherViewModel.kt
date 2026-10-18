package com.example.viewmodel

import android.app.Activity
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherPreferencesRepository
import com.example.data.LauncherUserPreferences
import com.example.data.SystemControlManager
import com.example.model.BatteryTelemetryState
import com.example.model.ClockFaceStyle
import com.example.model.ClockTimeState
import com.example.model.CoreSystemSettingsState
import com.example.model.EssentialAppSlot
import com.example.model.InstalledAppInfo
import com.example.model.LauncherDestination
import com.example.model.PhosphorTheme
import com.example.model.RingerProfile
import com.example.model.SavedNormalProfileSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EcoLauncherUiState(
    val currentDestination: LauncherDestination = LauncherDestination.HOME,
    val clockTime: ClockTimeState = ClockTimeState(),
    val batteryTelemetry: BatteryTelemetryState = BatteryTelemetryState(),
    val systemSettings: CoreSystemSettingsState = CoreSystemSettingsState(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val appSearchQuery: String = "",
    val slotSelectionTarget: EssentialAppSlot? = null,
    val showQuickCalculatorDialog: Boolean = false,
    val statusBannerMessage: String? = null
)

class EcoLauncherViewModel(
    private val preferencesRepository: LauncherPreferencesRepository,
    private val systemControlManager: SystemControlManager
) : ViewModel() {

    val userPreferences: StateFlow<LauncherUserPreferences> =
        preferencesRepository.preferencesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LauncherUserPreferences()
        )

    private val _uiState = MutableStateFlow(EcoLauncherUiState())
    val uiState: StateFlow<EcoLauncherUiState> = _uiState.asStateFlow()

    private val shortDateFormat = SimpleDateFormat("EEE, MMM dd", Locale.US)
    private val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)

    init {
        refreshSystemAndBatteryState()
        loadInstalledApps()
        startHighPrecisionClockTicker()
        startTelemetryPoller()
    }

    private fun startHighPrecisionClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                updateClockTime()
                delay(100L) // 10 Hz update for crisp seconds & tenths-of-second sweep
            }
        }
    }

    private fun startTelemetryPoller() {
        viewModelScope.launch {
            while (isActive) {
                refreshSystemAndBatteryState()
                delay(3_000L)
            }
        }
    }

    private fun updateClockTime() {
        val prefs = userPreferences.value
        val nowMs = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }

        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val displayHour = if (prefs.use24HourFormat) hour24 else hour12
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val millis = cal.get(Calendar.MILLISECOND)
        val tenths = millis / 100

        val amPm = if (prefs.use24HourFormat) {
            "24H"
        } else {
            if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        }

        val uptimeTotalSec = SystemClock.elapsedRealtime() / 1000L
        val upH = uptimeTotalSec / 3600L
        val upM = (uptimeTotalSec % 3600L) / 60L
        val upS = uptimeTotalSec % 60L
        val uptimeStr = String.format(Locale.US, "%02d:%02d:%02d", upH, upM, upS)

        // Subtle circular pixel-shift every minute for OLED Burn-In prevention (-4..+4 dp)
        val minuteBucket = (nowMs / 60_000L) % 12L
        val angleRad = (minuteBucket / 12.0) * 2.0 * Math.PI
        val shiftX = if (prefs.burnInProtectionEnabled) (cos(angleRad) * 4.0).roundToInt() else 0
        val shiftY = if (prefs.burnInProtectionEnabled) (sin(angleRad) * 4.0).roundToInt() else 0

        val dateObj = Date(nowMs)
        _uiState.update { current ->
            current.copy(
                clockTime = ClockTimeState(
                    hoursFormatted = String.format(Locale.US, "%02d", displayHour),
                    minutesFormatted = String.format(Locale.US, "%02d", minute),
                    secondsFormatted = String.format(Locale.US, "%02d", second),
                    tenthsFormatted = tenths.toString(),
                    amPm = amPm,
                    secondsInt = second,
                    millisInSecond = millis,
                    dateFormatted = shortDateFormat.format(dateObj).uppercase(Locale.US),
                    fullDateFormatted = fullDateFormat.format(dateObj),
                    uptimeFormatted = uptimeStr,
                    nextAlarmFormatted = current.clockTime.nextAlarmFormatted,
                    pixelShiftX = shiftX,
                    pixelShiftY = shiftY
                )
            )
        }
    }

    fun refreshSystemAndBatteryState() {
        val sys = systemControlManager.readCoreSystemSettings()
        val isUltra = userPreferences.value.isUltraPowerSaveActive
        val battery = systemControlManager.readBatteryTelemetry(
            isUltraMode = isUltra,
            currentBrightness = sys.systemBrightness
        )
        val nextAlarm = systemControlManager.readNextAlarmFormatted()

        _uiState.update { state ->
            state.copy(
                systemSettings = sys,
                batteryTelemetry = battery,
                clockTime = state.clockTime.copy(nextAlarmFormatted = nextAlarm)
            )
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = systemControlManager.queryInstalledLauncherApps()
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun navigateTo(destination: LauncherDestination) {
        _uiState.update { it.copy(currentDestination = destination, statusBannerMessage = null) }
        refreshSystemAndBatteryState()
    }

    fun onScreenWakeOrLocked(isDeviceLocked: Boolean) {
        refreshSystemAndBatteryState()
        if (userPreferences.value.autoShowOnLockScreen || isDeviceLocked) {
            _uiState.update { it.copy(currentDestination = LauncherDestination.LOCK_SCREEN) }
        }
    }

    fun toggleUltraPowerSaveMode() {
        viewModelScope.launch {
            val prefs = userPreferences.value
            val currentSys = systemControlManager.readCoreSystemSettings()
            val nextUltra = !prefs.isUltraPowerSaveActive

            if (nextUltra) {
                // Save current normal settings snapshot before applying aggressive power saving
                val snapshot = SavedNormalProfileSnapshot(
                    brightness = currentSys.systemBrightness,
                    autoBrightness = currentSys.isAutoBrightness,
                    timeoutMs = currentSys.screenOffTimeoutMs,
                    hapticEnabled = currentSys.hapticFeedbackEnabled,
                    soundEnabled = currentSys.soundEffectsEnabled,
                    mediaVolumePercent = currentSys.mediaVolumePercent
                )
                preferencesRepository.setUltraPowerSave(active = true, snapshotToSave = snapshot)
                preferencesRepository.setPureBlackOled(true)
                preferencesRepository.setBurnInProtection(true)

                // Modify core system settings to ultra-power-saving profile
                systemControlManager.setAutoBrightness(false)
                val wroteSys = systemControlManager.setSystemBrightness(18) // ~7% OLED luminance
                systemControlManager.setScreenOffTimeout(15_000) // 15s aggressive screen off
                systemControlManager.setHapticFeedbackEnabled(false)
                systemControlManager.setSoundEffectsEnabled(false)
                systemControlManager.setMediaVolumePercent(
                    currentSys.mediaVolumePercent.coerceAtMost(20)
                )

                val msg = if (wroteSys) {
                    "Ultra Power Save engaged: System brightness set to 7%, timeout 15s, haptics & touch audio muted."
                } else {
                    "Ultra Power Save engaged (Window dimmed to 7%). Grant 'Modify System Settings' to persist OS timeout & brightness."
                }
                _uiState.update { it.copy(statusBannerMessage = msg) }
            } else {
                // Restore saved normal profile snapshot
                val snap = prefs.savedNormalSnapshot
                preferencesRepository.setUltraPowerSave(active = false)
                systemControlManager.setSystemBrightness(snap.brightness)
                systemControlManager.setAutoBrightness(snap.autoBrightness)
                systemControlManager.setScreenOffTimeout(snap.timeoutMs)
                systemControlManager.setHapticFeedbackEnabled(snap.hapticEnabled)
                systemControlManager.setSoundEffectsEnabled(snap.soundEnabled)
                systemControlManager.setMediaVolumePercent(snap.mediaVolumePercent)

                _uiState.update {
                    it.copy(statusBannerMessage = "Normal power profile restored from baseline snapshot.")
                }
            }
            refreshSystemAndBatteryState()
        }
    }

    fun updateSystemBrightness(brightness: Int) {
        val wrote = systemControlManager.setSystemBrightness(brightness)
        refreshSystemAndBatteryState()
        if (!wrote) {
            _uiState.update {
                it.copy(
                    systemSettings = it.systemSettings.copy(
                        systemBrightness = brightness.coerceIn(1, 255),
                        isAutoBrightness = false
                    ),
                    statusBannerMessage = "Active window brightness updated. Grant 'Modify System Settings' to write global OS brightness."
                )
            }
        }
    }

    fun updateAutoBrightness(enabled: Boolean) {
        val wrote = systemControlManager.setAutoBrightness(enabled)
        refreshSystemAndBatteryState()
        if (!wrote) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Grant 'Modify System Settings' permission to switch OS Adaptive Brightness mode."
                )
            }
        }
    }

    fun updateScreenTimeout(timeoutMs: Int) {
        val wrote = systemControlManager.setScreenOffTimeout(timeoutMs)
        refreshSystemAndBatteryState()
        if (wrote) {
            val sec = timeoutMs / 1000
            _uiState.update {
                it.copy(statusBannerMessage = "System Screen Off Timeout updated to ${sec}s.")
            }
        } else {
            _uiState.update {
                it.copy(
                    systemSettings = it.systemSettings.copy(screenOffTimeoutMs = timeoutMs),
                    statusBannerMessage = "Grant 'Modify System Settings' permission to write OS screen timeout."
                )
            }
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        val wrote = systemControlManager.setHapticFeedbackEnabled(enabled)
        refreshSystemAndBatteryState()
        if (!wrote) {
            _uiState.update {
                it.copy(
                    systemSettings = it.systemSettings.copy(hapticFeedbackEnabled = enabled),
                    statusBannerMessage = "Grant 'Modify System Settings' to modify global haptic feedback."
                )
            }
        }
    }

    fun updateSoundEffects(enabled: Boolean) {
        val wrote = systemControlManager.setSoundEffectsEnabled(enabled)
        refreshSystemAndBatteryState()
        if (!wrote) {
            _uiState.update {
                it.copy(
                    systemSettings = it.systemSettings.copy(soundEffectsEnabled = enabled),
                    statusBannerMessage = "Grant 'Modify System Settings' to modify global touch sounds."
                )
            }
        }
    }

    fun updateMediaVolume(percent: Int) {
        systemControlManager.setMediaVolumePercent(percent)
        refreshSystemAndBatteryState()
    }

    fun updateRingerProfile(profile: RingerProfile) {
        val ok = systemControlManager.setRingerProfile(profile)
        refreshSystemAndBatteryState()
        if (!ok && profile == RingerProfile.SILENT) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Switched to Vibrate. Grant 'Do Not Disturb Access' for full Silent lock."
                )
            }
        }
    }

    fun toggleFlashlight() {
        val ok = systemControlManager.toggleFlashlight()
        refreshSystemAndBatteryState()
        if (!ok) {
            _uiState.update {
                it.copy(statusBannerMessage = "Hardware camera flash unit is unavailable on this device/emulator.")
            }
        }
    }

    fun setClockFaceStyle(style: ClockFaceStyle) {
        viewModelScope.launch {
            preferencesRepository.setClockFaceStyle(style)
        }
    }

    fun cycleClockFaceStyle() {
        val styles = ClockFaceStyle.entries
        val current = userPreferences.value.clockFaceStyle
        val next = styles[(styles.indexOf(current) + 1) % styles.size]
        setClockFaceStyle(next)
    }

    fun setPhosphorTheme(theme: PhosphorTheme) {
        viewModelScope.launch {
            preferencesRepository.setPhosphorTheme(theme)
        }
    }

    fun setUse24HourFormat(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUse24HourFormat(enabled)
            updateClockTime()
        }
    }

    fun setShowSubSecondTenths(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowSubSecondTenths(enabled)
        }
    }

    fun setBurnInProtection(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBurnInProtection(enabled)
        }
    }

    fun setAutoShowOnLockScreen(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoShowOnLockScreen(enabled)
        }
    }

    fun launchEssentialSlot(slot: EssentialAppSlot) {
        systemControlManager.launchAssignedOrDefaultSlot(
            slot = slot,
            onRequestBuiltInCalculator = {
                _uiState.update { it.copy(showQuickCalculatorDialog = true) }
            }
        )
    }

    fun dismissQuickCalculator() {
        _uiState.update { it.copy(showQuickCalculatorDialog = false) }
    }

    fun launchInstalledApp(app: InstalledAppInfo) {
        systemControlManager.launchApp(app.packageName, app.activityName)
    }

    fun openSlotAssignmentPicker(slot: EssentialAppSlot) {
        loadInstalledApps()
        _uiState.update {
            it.copy(
                slotSelectionTarget = slot,
                currentDestination = LauncherDestination.APP_DRAWER
            )
        }
    }

    fun cancelSlotAssignment() {
        _uiState.update { it.copy(slotSelectionTarget = null) }
    }

    fun assignAppToSlot(slotIndex: Int, app: InstalledAppInfo) {
        viewModelScope.launch {
            preferencesRepository.assignAppToSlot(
                slotIndex = slotIndex,
                label = app.label,
                packageName = app.packageName,
                activityName = app.activityName
            )
            _uiState.update {
                it.copy(
                    slotSelectionTarget = null,
                    currentDestination = LauncherDestination.HOME,
                    statusBannerMessage = "Pinned ${app.label} to Home Dock Slot #${slotIndex + 1}."
                )
            }
        }
    }

    fun resetSlotToDefault(slotIndex: Int) {
        viewModelScope.launch {
            preferencesRepository.resetSlotToDefault(slotIndex)
            _uiState.update {
                it.copy(
                    slotSelectionTarget = null,
                    statusBannerMessage = "Reset Slot #${slotIndex + 1} to default system role."
                )
            }
        }
    }

    fun updateAppSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun openWriteSettingsPermission() = systemControlManager.openWriteSettingsPermissionScreen()
    fun openDndPolicyPermission() = systemControlManager.openNotificationPolicyPermissionScreen()
    fun openDefaultHomeRole(activity: Activity?) = systemControlManager.openDefaultHomeSettings(activity)
    fun openWifiPanel() = systemControlManager.openWifiPanel()
    fun openSystemAction(action: String) = systemControlManager.openSystemAction(action)
    fun formatCurrentDrain(microAmps: Int): String = systemControlManager.formatCurrentDrainMa(microAmps)

    class Factory(
        private val preferencesRepository: LauncherPreferencesRepository,
        private val systemControlManager: SystemControlManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EcoLauncherViewModel(preferencesRepository, systemControlManager) as T
        }
    }
}
