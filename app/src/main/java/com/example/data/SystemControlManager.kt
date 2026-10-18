package com.example.data

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.role.RoleManager
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.model.BatteryTelemetryState
import com.example.model.CoreSystemSettingsState
import com.example.model.DefaultEssentialRole
import com.example.model.EssentialAppSlot
import com.example.model.InstalledAppInfo
import com.example.model.RingerProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class SystemControlManager(private val context: Context) {

    private val contentResolver = context.contentResolver
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var torchCameraId: String? = null
    private var currentTorchState: Boolean = false

    // Optional window brightness callback registered by MainActivity so brightness changes
    // also immediately reflect on the active window even before WRITE_SETTINGS is granted.
    var windowBrightnessUpdater: ((Float) -> Unit)? = null

    init {
        discoverTorchCamera()
    }

    private fun discoverTorchCamera() {
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    torchCameraId = id
                    break
                }
            }
        } catch (_: Exception) {
            torchCameraId = null
        }
    }

    fun canWriteSystemSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun hasNotificationPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun isDefaultHomeLauncher(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
            } else {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = context.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                resolveInfo?.activityInfo?.packageName == context.packageName
            }
        } catch (_: Exception) {
            false
        }
    }

    fun readCoreSystemSettings(): CoreSystemSettingsState {
        val canWrite = canWriteSystemSettings()
        val hasDnd = hasNotificationPolicyAccess()
        val isHome = isDefaultHomeLauncher()

        val brightness = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 60)
                .coerceIn(0, 255)
        } catch (_: Exception) {
            60
        }

        val autoBrightness = try {
            Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (_: Exception) {
            false
        }

        val timeoutMs = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)
                .coerceIn(5_000, 1_800_000)
        } catch (_: Exception) {
            30_000
        }

        val hapticEnabled = try {
            Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        } catch (_: Exception) {
            true
        }

        val soundEffectsEnabled = try {
            Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) == 1
        } catch (_: Exception) {
            false
        }

        val ringerProfile = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerProfile.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerProfile.VIBRATE
            else -> RingerProfile.NORMAL
        }

        val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaPercent = ((curMedia.toFloat() / maxMedia.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val curRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val ringPercent = ((curRing.toFloat() / maxRing.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        val wifiEnabled = try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.isWifiEnabled ?: false
        } catch (_: Exception) {
            false
        }

        val bluetoothEnabled = try {
            val btManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }

        val airplaneMode = try {
            Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        } catch (_: Exception) {
            false
        }

        return CoreSystemSettingsState(
            canWriteSystemSettings = canWrite,
            hasNotificationPolicyAccess = hasDnd,
            isDefaultHomeLauncher = isHome,
            systemBrightness = brightness,
            isAutoBrightness = autoBrightness,
            screenOffTimeoutMs = timeoutMs,
            hapticFeedbackEnabled = hapticEnabled,
            soundEffectsEnabled = soundEffectsEnabled,
            ringerProfile = ringerProfile,
            mediaVolumePercent = mediaPercent,
            ringVolumePercent = ringPercent,
            isFlashlightOn = currentTorchState,
            hasFlashHardware = torchCameraId != null,
            isSystemPowerSaveMode = powerManager.isPowerSaveMode,
            isWifiEnabled = wifiEnabled,
            isBluetoothEnabled = bluetoothEnabled,
            isAirplaneModeOn = airplaneMode,
            isDeviceLocked = keyguardManager.isKeyguardLocked
        )
    }

    fun setSystemBrightness(value: Int): Boolean {
        val clamped = value.coerceIn(1, 255)
        // Always update active window brightness immediately so the user sees instant visual feedback
        windowBrightnessUpdater?.invoke((clamped / 255f).coerceIn(0.02f, 1.0f))
        if (!canWriteSystemSettings()) return false
        return try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, clamped)
        } catch (_: Exception) {
            false
        }
    }

    fun setAutoBrightness(enabled: Boolean): Boolean {
        if (enabled) {
            // Release window override to system default
            windowBrightnessUpdater?.invoke(-1f)
        }
        if (!canWriteSystemSettings()) return false
        return try {
            val mode = if (enabled) {
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            } else {
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            }
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
        } catch (_: Exception) {
            false
        }
    }

    fun setScreenOffTimeout(timeoutMs: Int): Boolean {
        if (!canWriteSystemSettings()) return false
        return try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                timeoutMs.coerceIn(10_000, 1_800_000)
            )
        } catch (_: Exception) {
            false
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean): Boolean {
        if (!canWriteSystemSettings()) return false
        return try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                if (enabled) 1 else 0
            )
        } catch (_: Exception) {
            false
        }
    }

    fun setSoundEffectsEnabled(enabled: Boolean): Boolean {
        if (!canWriteSystemSettings()) return false
        return try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SOUND_EFFECTS_ENABLED,
                if (enabled) 1 else 0
            )
        } catch (_: Exception) {
            false
        }
    }

    fun setMediaVolumePercent(percent: Int) {
        try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val target = ((percent.coerceIn(0, 100) / 100f) * max).roundToInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {
        }
    }

    fun setRingerProfile(profile: RingerProfile): Boolean {
        return try {
            if (profile == RingerProfile.SILENT && !hasNotificationPolicyAccess()) {
                // Fallback to vibrate if DND policy is not yet granted
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                false
            } else {
                audioManager.ringerMode = when (profile) {
                    RingerProfile.SILENT -> AudioManager.RINGER_MODE_SILENT
                    RingerProfile.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                    RingerProfile.NORMAL -> AudioManager.RINGER_MODE_NORMAL
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun toggleFlashlight(): Boolean {
        val camId = torchCameraId ?: return false
        return try {
            val nextState = !currentTorchState
            cameraManager.setTorchMode(camId, nextState)
            currentTorchState = nextState
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBatteryTelemetry(isUltraMode: Boolean, currentBrightness: Int): BatteryTelemetryState {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 85

        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val powerSource = when {
            plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "AC Fast Charge"
            plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB Power"
            plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Wireless Qi"
            isCharging -> "Charging"
            else -> "OLED Battery Cell"
        }

        val tempTenths = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 285) ?: 285
        val tempC = tempTenths / 10f
        val voltageMv = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100) ?: 4100

        val currentNowUa = try {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } catch (_: Exception) {
            -165_000
        }

        // Calculate realistic runtime projections based on battery % and brightness/ultra state
        val brightnessFactor = 1f - ((255 - currentBrightness.coerceIn(1, 255)) / 255f) * 0.35f
        val baseMinutesPerPercent = 6.2f / brightnessFactor.coerceAtLeast(0.55f)
        val normalMinutes = (pct * baseMinutesPerPercent).roundToInt().coerceAtLeast(15)
        val ultraMinutes = (pct * baseMinutesPerPercent * 2.35f).roundToInt().coerceAtLeast(35)

        return BatteryTelemetryState(
            levelPercent = pct,
            isCharging = isCharging,
            powerSourceLabel = powerSource,
            temperatureCelsius = tempC,
            voltageMillivolts = if (voltageMv < 100) voltageMv * 1000 else voltageMv,
            currentNowMicroAmps = if (currentNowUa == 0 || currentNowUa == Int.MIN_VALUE) {
                if (isUltraMode) -92_000 else -215_000
            } else {
                currentNowUa
            },
            estimatedNormalMinutes = normalMinutes,
            estimatedUltraMinutes = ultraMinutes
        )
    }

    fun readNextAlarmFormatted(): String {
        return try {
            val next = alarmManager.nextAlarmClock
            if (next != null && next.triggerTime > 0) {
                val fmt = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                "ALARM ${fmt.format(Date(next.triggerTime)).uppercase(Locale.getDefault())}"
            } else {
                "NO ALARM SET"
            }
        } catch (_: Exception) {
            "NO ALARM SET"
        }
    }

    fun queryInstalledLauncherApps(): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            resolveInfos
                .mapNotNull { ri ->
                    val ai = ri.activityInfo ?: return@mapNotNull null
                    if (ai.packageName == context.packageName) return@mapNotNull null
                    val label = ri.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                        ?: ai.packageName
                    val isSys = (ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    InstalledAppInfo(
                        label = label,
                        packageName = ai.packageName,
                        activityName = ai.name,
                        isSystemApp = isSys
                    )
                }
                .distinctBy { "${it.packageName}/${it.activityName}" }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchAssignedOrDefaultSlot(
        slot: EssentialAppSlot,
        onRequestBuiltInCalculator: () -> Unit
    ): Boolean {
        // 1. If user assigned a custom package/activity to this slot, launch it first
        if (!slot.assignedPackageName.isNullOrBlank()) {
            val launched = launchApp(slot.assignedPackageName, slot.assignedActivityName)
            if (launched) return true
        }

        // 2. Otherwise launch the role's default system intent
        return when (slot.role) {
            DefaultEssentialRole.DIALER -> {
                launchSafeIntent(Intent(Intent.ACTION_DIAL))
            }
            DefaultEssentialRole.MESSAGES -> {
                val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                }
                if (!launchSafeIntent(smsIntent)) {
                    launchSafeIntent(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
                } else {
                    true
                }
            }
            DefaultEssentialRole.CLOCK_ALARM -> {
                if (!launchSafeIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS))) {
                    launchSafeIntent(Intent(AlarmClock.ACTION_SET_ALARM))
                } else {
                    true
                }
            }
            DefaultEssentialRole.CALCULATOR -> {
                val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                }
                if (!launchSafeIntent(calcIntent)) {
                    onRequestBuiltInCalculator()
                }
                true
            }
            DefaultEssentialRole.CAMERA -> {
                if (!launchSafeIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))) {
                    launchSafeIntent(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                } else {
                    true
                }
            }
            DefaultEssentialRole.SETTINGS -> {
                launchSafeIntent(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    fun launchApp(packageName: String, activityName: String? = null): Boolean {
        return try {
            val intent = if (!activityName.isNullOrBlank()) {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openWriteSettingsPermissionScreen() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        if (!launchSafeIntent(intent)) {
            launchSafeIntent(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openNotificationPolicyPermissionScreen() {
        if (!launchSafeIntent(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))) {
            launchSafeIntent(Intent(Settings.ACTION_SOUND_SETTINGS))
        }
    }

    fun openDefaultHomeSettings(activity: Activity? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
            try {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null &&
                    roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                ) {
                    val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    activity.startActivityForResult(roleIntent, 901)
                    return
                }
            } catch (_: Exception) {
            }
        }
        if (!launchSafeIntent(Intent(Settings.ACTION_HOME_SETTINGS))) {
            launchSafeIntent(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openSystemAction(action: String) {
        if (!launchSafeIntent(Intent(action))) {
            launchSafeIntent(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openWifiPanel() {
        val panelIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        if (!launchSafeIntent(panelIntent)) {
            launchSafeIntent(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    private fun launchSafeIntent(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun formatCurrentDrainMa(microAmps: Int): String {
        val ma = abs(microAmps) / 1000
        return "${ma.coerceIn(45, 1800)} mA"
    }
}
