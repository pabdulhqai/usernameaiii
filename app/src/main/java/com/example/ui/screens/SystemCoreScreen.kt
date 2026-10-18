package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LauncherUserPreferences
import com.example.model.ClockFaceStyle
import com.example.model.CoreSystemSettingsState
import com.example.model.PhosphorTheme
import com.example.model.RingerProfile
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemCoreScreen(
    systemSettings: CoreSystemSettingsState,
    preferences: LauncherUserPreferences,
    onBrightnessChange: (Int) -> Unit,
    onAutoBrightnessChange: (Boolean) -> Unit,
    onScreenTimeoutChange: (Int) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onSoundEffectsChange: (Boolean) -> Unit,
    onMediaVolumeChange: (Int) -> Unit,
    onRingerProfileChange: (RingerProfile) -> Unit,
    onSelectPhosphorTheme: (PhosphorTheme) -> Unit,
    onSelectClockStyle: (ClockFaceStyle) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onToggleTenths: (Boolean) -> Unit,
    onToggleBurnInProtection: (Boolean) -> Unit,
    onToggleAutoShowOnLock: (Boolean) -> Unit,
    onOpenWriteSettingsPermission: () -> Unit,
    onOpenDndPermission: () -> Unit,
    onOpenSystemAction: (String) -> Unit,
    onRefreshSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("system_core_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CORE SYSTEM SETTINGS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Direct OS hardware luminance, timeout, audio & OLED clock controls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
        }

        // 1. WRITE_SETTINGS Permission Status Card
        SystemWritePermissionCard(
            canWrite = systemSettings.canWriteSystemSettings,
            hasDndAccess = systemSettings.hasNotificationPolicyAccess,
            accent = accent,
            dimAccent = dimAccent,
            onGrantWriteSettings = onOpenWriteSettingsPermission,
            onGrantDnd = onOpenDndPermission,
            onRefresh = onRefreshSettings
        )

        // 2. Display & Screen Timeout Governor
        CoreSectionCard(title = "DISPLAY & LUMINANCE GOVERNOR", accent = accent, dimAccent = dimAccent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Screen Brightness",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${systemSettings.brightnessPercent}% (${systemSettings.systemBrightness}/255)",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }

            Slider(
                value = systemSettings.systemBrightness.toFloat(),
                onValueChange = { onBrightnessChange(it.roundToInt()) },
                valueRange = 1f..255f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = dimAccent
                ),
                modifier = Modifier.testTag("system_brightness_slider")
            )

            SettingSwitchRow(
                title = "Adaptive / Auto Brightness Mode",
                subtitle = "Disable in Ultra Power Save to lock ultra-low OLED current",
                checked = systemSettings.isAutoBrightness,
                onCheckedChange = onAutoBrightnessChange,
                accent = accent,
                testTag = "auto_brightness_switch"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "System Screen-Off Timeout",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            val timeoutOptions = listOf(
                15_000 to "15 sec",
                30_000 to "30 sec",
                60_000 to "1 min",
                120_000 to "2 min",
                300_000 to "5 min",
                600_000 to "10 min"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                timeoutOptions.forEach { (ms, label) ->
                    val selected = systemSettings.screenOffTimeoutMs == ms
                    FilterChip(
                        selected = selected,
                        onClick = { onScreenTimeoutChange(ms) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF0A120E),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("timeout_chip_${ms / 1000}")
                    )
                }
            }
        }

        // 3. System Haptics, Audio & Ringer Control
        CoreSectionCard(title = "AUDIO, HAPTICS & RINGER POLICY", accent = accent, dimAccent = dimAccent) {
            Text(
                text = "System Ringer Profile",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RingerProfile.entries.forEach { profile ->
                    val selected = systemSettings.ringerProfile == profile
                    FilterChip(
                        selected = selected,
                        onClick = { onRingerProfileChange(profile) },
                        label = { Text(profile.label, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Media Stream Volume",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Text(
                    text = "${systemSettings.mediaVolumePercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }

            Slider(
                value = systemSettings.mediaVolumePercent.toFloat(),
                onValueChange = { onMediaVolumeChange(it.roundToInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = dimAccent
                ),
                modifier = Modifier.testTag("media_volume_slider")
            )

            SettingSwitchRow(
                title = "System Haptic Feedback",
                subtitle = "Disable vibration motor pulses on tap to save ~15mW per interaction",
                checked = systemSettings.hapticFeedbackEnabled,
                onCheckedChange = onHapticFeedbackChange,
                accent = accent,
                testTag = "haptic_feedback_switch"
            )

            SettingSwitchRow(
                title = "System Touch Sound Effects",
                checked = systemSettings.soundEffectsEnabled,
                subtitle = "Keep speaker amplifier asleep during navigation taps",
                onCheckedChange = onSoundEffectsChange,
                accent = accent,
                testTag = "sound_effects_switch"
            )
        }

        // 4. OLED Subpixel Phosphor Theme & Large Clock Architecture
        CoreSectionCard(title = "OLED CLOCK & SUBPIXEL PHOSPHOR", accent = accent, dimAccent = dimAccent) {
            Text(
                text = "OLED Subpixel Color Wavelength",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PhosphorTheme.entries.forEach { theme ->
                    val isSelected = preferences.phosphorTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.dimColor.copy(alpha = 0.35f) else Color(0xFF070C0A))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) theme.primaryColor else dimAccent.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectPhosphorTheme(theme) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("phosphor_theme_${theme.id}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = theme.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = theme.subpixelEfficiency,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.primaryColor
                                )
                            }
                        }
                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelLarge,
                                color = theme.primaryColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Large Clock Architecture",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            ClockFaceStyle.entries.forEach { style ->
                val selected = preferences.clockFaceStyle == style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) dimAccent.copy(alpha = 0.35f) else Color(0xFF070C0A))
                        .border(
                            width = 1.dp,
                            color = if (selected) accent else dimAccent.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectClockStyle(style) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(style.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            style.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingSwitchRow(
                title = "24-Hour Military Time",
                subtitle = "Display 00:00:00–23:59:59 format",
                checked = preferences.use24HourFormat,
                onCheckedChange = onToggle24Hour,
                accent = accent,
                testTag = "switch_24h_format"
            )

            SettingSwitchRow(
                title = "Sub-Second Tenths (.0s – .9s)",
                subtitle = "High-frequency 10Hz chronometer decimal readout",
                checked = preferences.showSubSecondTenths,
                onCheckedChange = onToggleTenths,
                accent = accent,
                testTag = "switch_tenths"
            )

            SettingSwitchRow(
                title = "OLED Burn-In Pixel Orbit",
                subtitle = "Shifts clock ±4dp every 60 seconds to protect AMOLED organic diodes",
                checked = preferences.burnInProtectionEnabled,
                onCheckedChange = onToggleBurnInProtection,
                accent = accent,
                testTag = "switch_burn_in"
            )

            SettingSwitchRow(
                title = "Show Clock Automatically on Lock Screen",
                subtitle = "Present EcoClock immediately over the keyguard when waking screen",
                checked = preferences.autoShowOnLockScreen,
                onCheckedChange = onToggleAutoShowOnLock,
                accent = accent,
                testTag = "switch_auto_lock_screen"
            )
        }

        // 5. Direct Android OS System Consoles
        CoreSectionCard(title = "OS RADIO & BATTERY SAVER BRIDGES", accent = accent, dimAccent = dimAccent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OsShortcutButton(
                    icon = Icons.Filled.BatterySaver,
                    label = "OS Battery Saver",
                    onClick = { onOpenSystemAction(Settings.ACTION_BATTERY_SAVER_SETTINGS) },
                    accent = accent,
                    dimAccent = dimAccent,
                    modifier = Modifier.weight(1f)
                )
                OsShortcutButton(
                    icon = Icons.Filled.Wifi,
                    label = "Wi-Fi Settings",
                    onClick = { onOpenSystemAction(Settings.ACTION_WIFI_SETTINGS) },
                    accent = accent,
                    dimAccent = dimAccent,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OsShortcutButton(
                    icon = Icons.Filled.Bluetooth,
                    label = "Bluetooth Radio",
                    onClick = { onOpenSystemAction(Settings.ACTION_BLUETOOTH_SETTINGS) },
                    accent = accent,
                    dimAccent = dimAccent,
                    modifier = Modifier.weight(1f)
                )
                OsShortcutButton(
                    icon = Icons.Filled.SettingsApplications,
                    label = "Display Settings",
                    onClick = { onOpenSystemAction(Settings.ACTION_DISPLAY_SETTINGS) },
                    accent = accent,
                    dimAccent = dimAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SystemWritePermissionCard(
    canWrite: Boolean,
    hasDndAccess: Boolean,
    accent: Color,
    dimAccent: Color,
    onGrantWriteSettings: () -> Unit,
    onGrantDnd: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = Color(0xFF060C09),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (canWrite) accent else Color(0xFFFFB300),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (canWrite) Icons.Filled.CheckCircle else Icons.Filled.Security,
                        contentDescription = null,
                        tint = if (canWrite) accent else Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canWrite) {
                            "WRITE_SETTINGS PRIVILEGE GRANTED"
                        } else {
                            "SYSTEM SETTINGS PERMISSION REQUIRED"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (canWrite) accent else Color(0xFFFFB300)
                    )
                }

                Text(
                    text = "SYNC",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(dimAccent.copy(alpha = 0.35f))
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (canWrite) {
                    "EcoClock has direct write access to Android Settings.System (OS brightness, adaptive brightness mode, screen timeout, haptics, and touch audio)."
                } else {
                    "Grant 'Modify System Settings' so EcoClock can persist OS-wide screen timeout, brightness mode, haptics, and touch audio changes."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGrantWriteSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canWrite) dimAccent else Color(0xFFFFB300),
                        contentColor = if (canWrite) accent else Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manage_write_settings_button")
                ) {
                    Text(
                        text = if (canWrite) "Manage Write Access" else "Grant Write Settings",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!hasDndAccess) {
                    Button(
                        onClick = onGrantDnd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF121C17),
                            contentColor = accent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, dimAccent, RoundedCornerShape(10.dp))
                    ) {
                        Text(text = "DND Access", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreSectionCard(
    title: String,
    accent: Color,
    dimAccent: Color,
    content: @Composable () -> Unit
) {
    Surface(
        color = Color(0xFF040806),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, dimAccent.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun OsShortcutButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    accent: Color,
    dimAccent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF09110D))
            .border(1.dp, dimAccent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}
