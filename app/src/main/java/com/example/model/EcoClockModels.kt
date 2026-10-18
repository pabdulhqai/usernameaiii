package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PhosphorAmber
import com.example.ui.theme.PhosphorAmberDim
import com.example.ui.theme.PhosphorCyan
import com.example.ui.theme.PhosphorCyanDim
import com.example.ui.theme.PhosphorEmerald
import com.example.ui.theme.PhosphorEmeraldDim
import com.example.ui.theme.PhosphorMonochrome
import com.example.ui.theme.PhosphorMonochromeDim
import com.example.ui.theme.PhosphorRedShift
import com.example.ui.theme.PhosphorRedShiftDim

enum class LauncherDestination {
    HOME,
    SYSTEM_CORE,
    LOCK_SCREEN,
    APP_DRAWER
}

enum class ClockFaceStyle(val label: String, val subtitle: String) {
    CHRONO_SPLIT("Chrono Matrix", "Giant HH:MM with boxed seconds & 60-tick bar"),
    ORBITAL_RING("Orbital Sweep", "60-second luminous perimeter dial + tabular digital core"),
    VERTICAL_STACK("Bedside Tower", "Full-height stacked Hours, Minutes & Seconds")
}

enum class PhosphorTheme(
    val id: String,
    val displayName: String,
    val subpixelEfficiency: String,
    val primaryColor: Color,
    val dimColor: Color
) {
    EMERALD(
        id = "EMERALD",
        displayName = "Emerald Matrix",
        subpixelEfficiency = "94% OLED Save",
        primaryColor = PhosphorEmerald,
        dimColor = PhosphorEmeraldDim
    ),
    RED_SHIFT(
        id = "RED_SHIFT",
        displayName = "Crimson Red-Shift",
        subpixelEfficiency = "97% Max Save",
        primaryColor = PhosphorRedShift,
        dimColor = PhosphorRedShiftDim
    ),
    AMBER(
        id = "AMBER",
        displayName = "Amber Tactical",
        subpixelEfficiency = "93% OLED Save",
        primaryColor = PhosphorAmber,
        dimColor = PhosphorAmberDim
    ),
    CYAN(
        id = "CYAN",
        displayName = "Cryo Cyan",
        subpixelEfficiency = "91% OLED Save",
        primaryColor = PhosphorCyan,
        dimColor = PhosphorCyanDim
    ),
    MONOCHROME(
        id = "MONOCHROME",
        displayName = "E-Ink Slate",
        subpixelEfficiency = "89% OLED Save",
        primaryColor = PhosphorMonochrome,
        dimColor = PhosphorMonochromeDim
    );

    companion object {
        fun fromId(id: String): PhosphorTheme =
            entries.firstOrNull { it.id == id } ?: EMERALD
    }
}

data class ClockTimeState(
    val hoursFormatted: String = "00",
    val minutesFormatted: String = "00",
    val secondsFormatted: String = "00",
    val tenthsFormatted: String = "0",
    val amPm: String = "",
    val secondsInt: Int = 0,
    val millisInSecond: Int = 0,
    val dateFormatted: String = "SUN, OCT 18",
    val fullDateFormatted: String = "Sunday, October 18, 2026",
    val uptimeFormatted: String = "00:00:00",
    val nextAlarmFormatted: String = "No active alarm",
    // Burn-in protection subtle pixel offset (-4..4 dp)
    val pixelShiftX: Int = 0,
    val pixelShiftY: Int = 0
)

data class BatteryTelemetryState(
    val levelPercent: Int = 85,
    val isCharging: Boolean = false,
    val powerSourceLabel: String = "Discharging",
    val temperatureCelsius: Float = 28.4f,
    val voltageMillivolts: Int = 4120,
    val currentNowMicroAmps: Int = -180000,
    val estimatedNormalMinutes: Int = 420,
    val estimatedUltraMinutes: Int = 940
)

enum class RingerProfile(val modeValue: Int, val label: String) {
    NORMAL(2, "Normal"),
    VIBRATE(1, "Vibrate"),
    SILENT(0, "Silent")
}

data class CoreSystemSettingsState(
    val canWriteSystemSettings: Boolean = false,
    val hasNotificationPolicyAccess: Boolean = false,
    val isDefaultHomeLauncher: Boolean = false,
    val systemBrightness: Int = 45, // 0..255
    val isAutoBrightness: Boolean = false,
    val screenOffTimeoutMs: Int = 30_000, // 15s, 30s, 60s, 120s, 300s, 600s
    val hapticFeedbackEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = false,
    val ringerProfile: RingerProfile = RingerProfile.NORMAL,
    val mediaVolumePercent: Int = 40,
    val ringVolumePercent: Int = 70,
    val isFlashlightOn: Boolean = false,
    val hasFlashHardware: Boolean = true,
    val isSystemPowerSaveMode: Boolean = false,
    val isWifiEnabled: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
    val isDeviceLocked: Boolean = false
) {
    val brightnessPercent: Int
        get() = ((systemBrightness.coerceIn(0, 255) / 255f) * 100f).toInt()
}

data class SavedNormalProfileSnapshot(
    val brightness: Int = 140,
    val autoBrightness: Boolean = true,
    val timeoutMs: Int = 60_000,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val mediaVolumePercent: Int = 55
)

enum class DefaultEssentialRole(
    val slotIndex: Int,
    val defaultTitle: String,
    val subtitle: String
) {
    DIALER(0, "Phone", "Voice & Emergency"),
    MESSAGES(1, "Messages", "SMS & MMS"),
    CLOCK_ALARM(2, "Alarms", "System Clock"),
    CALCULATOR(3, "Calculator", "Quick Math"),
    CAMERA(4, "Camera", "Capture"),
    SETTINGS(5, "Settings", "Android OS")
}

data class EssentialAppSlot(
    val slotIndex: Int,
    val role: DefaultEssentialRole,
    val customLabel: String? = null,
    val assignedPackageName: String? = null,
    val assignedActivityName: String? = null
) {
    val displayTitle: String
        get() = customLabel ?: role.defaultTitle
}

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isSystemApp: Boolean
)
