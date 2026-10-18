package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LauncherUserPreferences
import com.example.model.BatteryTelemetryState
import com.example.model.ClockTimeState
import com.example.model.CoreSystemSettingsState
import com.example.model.DefaultEssentialRole
import com.example.model.EssentialAppSlot
import com.example.model.RingerProfile
import com.example.ui.components.LargePrecisionClock
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    clockTime: ClockTimeState,
    batteryTelemetry: BatteryTelemetryState,
    systemSettings: CoreSystemSettingsState,
    preferences: LauncherUserPreferences,
    currentDrainFormatted: String,
    onToggleUltraPowerSave: () -> Unit,
    onCycleClockStyle: () -> Unit,
    onToggle24Hour: () -> Unit,
    onQuickBrightnessStep: () -> Unit,
    onQuickTimeoutStep: () -> Unit,
    onCycleRinger: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenWifiPanel: () -> Unit,
    onEnterLockScreenStandby: () -> Unit,
    onLaunchSlot: (EssentialAppSlot) -> Unit,
    onAssignSlot: (EssentialAppSlot) -> Unit,
    onSetDefaultLauncher: (Activity?) -> Unit,
    onOpenWriteSettingsPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("home_screen_container"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Telemetry & Launcher Role Header
        TelemetryStatusHeader(
            battery = batteryTelemetry,
            isUltraActive = preferences.isUltraPowerSaveActive,
            isDefaultLauncher = systemSettings.isDefaultHomeLauncher,
            currentDrainFormatted = currentDrainFormatted,
            onSetDefaultLauncher = { onSetDefaultLauncher(activity) }
        )

        // 2. Hero Large Clock (Hours, Minutes, Seconds)
        LargePrecisionClock(
            clockTime = clockTime,
            clockStyle = preferences.clockFaceStyle,
            use24Hour = preferences.use24HourFormat,
            showTenths = preferences.showSubSecondTenths,
            burnInShiftEnabled = preferences.burnInProtectionEnabled,
            onCycleStyle = onCycleClockStyle,
            onToggle24Hour = onToggle24Hour
        )

        // 3. Master Ultra-Power-Saving Reactor Card
        UltraPowerSaveMasterCard(
            isUltraActive = preferences.isUltraPowerSaveActive,
            battery = batteryTelemetry,
            canWriteSettings = systemSettings.canWriteSystemSettings,
            brightnessPercent = systemSettings.brightnessPercent,
            timeoutSeconds = systemSettings.screenOffTimeoutMs / 1000,
            onToggleUltra = onToggleUltraPowerSave,
            onGrantWritePermission = onOpenWriteSettingsPermission
        )

        // 4. Quick Core System Settings Strip
        QuickSystemControlsStrip(
            systemSettings = systemSettings,
            onQuickBrightnessStep = onQuickBrightnessStep,
            onQuickTimeoutStep = onQuickTimeoutStep,
            onCycleRinger = onCycleRinger,
            onToggleFlashlight = onToggleFlashlight,
            onOpenWifiPanel = onOpenWifiPanel,
            onEnterLockScreenStandby = onEnterLockScreenStandby
        )

        // 5. Essential Power-Saving App Dock (6 Customizable Slots)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESSENTIAL LAUNCH DOCK",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
                Text(
                    text = "TAP TO LAUNCH • EDIT ICON TO REASSIGN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val rows = preferences.essentialSlots.chunked(3)
            rows.forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowSlots.forEach { slot ->
                        EssentialDockTile(
                            slot = slot,
                            accent = accent,
                            dimAccent = dimAccent,
                            onClick = { onLaunchSlot(slot) },
                            onEditSlot = { onAssignSlot(slot) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TelemetryStatusHeader(
    battery: BatteryTelemetryState,
    isUltraActive: Boolean,
    isDefaultLauncher: Boolean,
    currentDrainFormatted: String,
    onSetDefaultLauncher: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer
    val activeMinutes = if (isUltraActive) battery.estimatedUltraMinutes else battery.estimatedNormalMinutes
    val estHours = activeMinutes / 60
    val estMins = activeMinutes % 60

    Surface(
        color = Color(0xFF050A08),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, dimAccent.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (battery.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatterySaver,
                    contentDescription = "Battery Status",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${battery.levelPercent}%",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.testTag("battery_percentage_text")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EST. ${estHours}H ${estMins}M",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                    }
                    Text(
                        text = "${battery.powerSourceLabel} • $currentDrainFormatted • ${battery.temperatureCelsius}°C",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isDefaultLauncher) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(dimAccent.copy(alpha = 0.4f))
                        .border(1.dp, accent, RoundedCornerShape(8.dp))
                        .clickable(onClick = onSetDefaultLauncher)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("set_default_launcher_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Set Default Home Launcher",
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SET HOME",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(dimAccent.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "HOME ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun UltraPowerSaveMasterCard(
    isUltraActive: Boolean,
    battery: BatteryTelemetryState,
    canWriteSettings: Boolean,
    brightnessPercent: Int,
    timeoutSeconds: Int,
    onToggleUltra: () -> Unit,
    onGrantWritePermission: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer
    val bonusMinutes = (battery.estimatedUltraMinutes - battery.estimatedNormalMinutes).coerceAtLeast(30)
    val bonusH = bonusMinutes / 60
    val bonusM = bonusMinutes % 60

    Surface(
        color = if (isUltraActive) dimAccent.copy(alpha = 0.22f) else Color(0xFF060B09),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isUltraActive) 1.5.dp else 1.dp,
                color = if (isUltraActive) accent else dimAccent,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isUltraActive) accent else dimAccent.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = if (isUltraActive) Color.Black else accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isUltraActive) "ULTRA POWER SAVE ENGAGED" else "ULTRA POWER SAVING GOVERNOR",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isUltraActive) accent else Color.White
                        )
                        Text(
                            text = "Gain +${bonusH}h ${bonusM}m • Lum ${brightnessPercent}% • Timeout ${timeoutSeconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onToggleUltra,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUltraActive) Color(0xFF14241C) else accent,
                        contentColor = if (isUltraActive) accent else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = accent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("toggle_ultra_power_save_button")
                ) {
                    Text(
                        text = if (isUltraActive) "RESTORE" else "ENGAGE",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (!canWriteSettings) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161105))
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onGrantWritePermission)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("grant_write_settings_banner"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Allow 'Modify System Settings' for OS-wide timeout & brightness control",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD54F),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRANT →",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFB300)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSystemControlsStrip(
    systemSettings: CoreSystemSettingsState,
    onQuickBrightnessStep: () -> Unit,
    onQuickTimeoutStep: () -> Unit,
    onCycleRinger: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenWifiPanel: () -> Unit,
    onEnterLockScreenStandby: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickTogglePill(
            icon = Icons.Outlined.BrightnessLow,
            label = "${systemSettings.brightnessPercent}%",
            subLabel = "BRIGHT",
            isActive = systemSettings.brightnessPercent <= 25,
            accent = accent,
            dimAccent = dimAccent,
            onClick = onQuickBrightnessStep,
            modifier = Modifier.weight(1f),
            testTag = "quick_brightness_button"
        )
        QuickTogglePill(
            icon = Icons.Outlined.Timer,
            label = "${systemSettings.screenOffTimeoutMs / 1000}s",
            subLabel = "SLEEP",
            isActive = systemSettings.screenOffTimeoutMs <= 30_000,
            accent = accent,
            dimAccent = dimAccent,
            onClick = onQuickTimeoutStep,
            modifier = Modifier.weight(1f),
            testTag = "quick_timeout_button"
        )
        QuickTogglePill(
            icon = when (systemSettings.ringerProfile) {
                RingerProfile.SILENT -> Icons.Filled.NotificationsOff
                RingerProfile.VIBRATE -> Icons.Filled.Vibration
                RingerProfile.NORMAL -> Icons.Filled.NotificationsActive
            },
            label = systemSettings.ringerProfile.label.take(4).uppercase(),
            subLabel = "RINGER",
            isActive = systemSettings.ringerProfile != RingerProfile.NORMAL,
            accent = accent,
            dimAccent = dimAccent,
            onClick = onCycleRinger,
            modifier = Modifier.weight(1f),
            testTag = "quick_ringer_button"
        )
        QuickTogglePill(
            icon = if (systemSettings.isFlashlightOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
            label = if (systemSettings.isFlashlightOn) "ON" else "OFF",
            subLabel = "TORCH",
            isActive = systemSettings.isFlashlightOn,
            accent = accent,
            dimAccent = dimAccent,
            onClick = onToggleFlashlight,
            modifier = Modifier.weight(1f),
            testTag = "quick_torch_button"
        )
        QuickTogglePill(
            icon = Icons.Filled.Wifi,
            label = if (systemSettings.isWifiEnabled) "ON" else "OFF",
            subLabel = "WI-FI",
            isActive = systemSettings.isWifiEnabled,
            accent = accent,
            dimAccent = dimAccent,
            onClick = onOpenWifiPanel,
            modifier = Modifier.weight(1f),
            testTag = "quick_wifi_button"
        )
        QuickTogglePill(
            icon = Icons.Filled.Lock,
            label = "LOCK",
            subLabel = "AOD",
            isActive = true,
            accent = accent,
            dimAccent = dimAccent,
            onClick = onEnterLockScreenStandby,
            modifier = Modifier.weight(1f),
            testTag = "quick_lock_standby_button"
        )
    }
}

@Composable
private fun QuickTogglePill(
    icon: ImageVector,
    label: String,
    subLabel: String,
    isActive: Boolean,
    accent: Color,
    dimAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) dimAccent.copy(alpha = 0.35f) else Color(0xFF060A08))
            .border(
                width = 1.dp,
                color = if (isActive) accent else dimAccent.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = subLabel,
            tint = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color.White
        )
        Text(
            text = subLabel,
            fontSize = 9.sp,
            fontFamily = JetBrainsMonoFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EssentialDockTile(
    slot: EssentialAppSlot,
    accent: Color,
    dimAccent: Color,
    onClick: () -> Unit,
    onEditSlot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (slot.role) {
        DefaultEssentialRole.DIALER -> Icons.Filled.Call
        DefaultEssentialRole.MESSAGES -> Icons.Filled.Message
        DefaultEssentialRole.CLOCK_ALARM -> Icons.Outlined.AccessAlarm
        DefaultEssentialRole.CALCULATOR -> Icons.Filled.Calculate
        DefaultEssentialRole.CAMERA -> Icons.Filled.CameraAlt
        DefaultEssentialRole.SETTINGS -> Icons.Filled.Settings
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF050A08))
            .border(1.dp, dimAccent.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onEditSlot
            )
            .padding(10.dp)
            .testTag("essential_slot_${slot.slotIndex}")
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(dimAccent.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = slot.displayTitle,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onEditSlot,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("edit_slot_${slot.slotIndex}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Assign app to slot ${slot.slotIndex + 1}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = slot.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (slot.assignedPackageName != null) "Custom Pinned" else slot.role.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
