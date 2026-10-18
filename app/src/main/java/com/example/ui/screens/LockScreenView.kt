package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LauncherUserPreferences
import com.example.model.BatteryTelemetryState
import com.example.model.ClockTimeState
import com.example.model.CoreSystemSettingsState
import com.example.ui.components.LargePrecisionClock
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun LockScreenView(
    clockTime: ClockTimeState,
    batteryTelemetry: BatteryTelemetryState,
    systemSettings: CoreSystemSettingsState,
    preferences: LauncherUserPreferences,
    currentDrainFormatted: String,
    onUnlockToHome: () -> Unit,
    onCycleClockStyle: () -> Unit,
    onToggle24Hour: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onEmergencyDialer: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onUnlockToHome)

    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer
    val estMinutes = if (preferences.isUltraPowerSaveActive) {
        batteryTelemetry.estimatedUltraMinutes
    } else {
        batteryTelemetry.estimatedNormalMinutes
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("lock_screen_view")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Minimal OLED Lock Status Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF060B09))
                        .border(1.dp, dimAccent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (preferences.burnInProtectionEnabled) {
                            "LOCK AOD • PIXEL ORBIT (${clockTime.pixelShiftX},${clockTime.pixelShiftY})"
                        } else {
                            "LOCK SCREEN STANDBY"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF060B09))
                        .border(1.dp, dimAccent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BatterySaver,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${batteryTelemetry.levelPercent}% • ${estMinutes / 60}h ${estMinutes % 60}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Centerpiece Large Clock (Hours, Minutes, Seconds) with Burn-In Pixel Shift
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                LargePrecisionClock(
                    clockTime = clockTime,
                    clockStyle = preferences.clockFaceStyle,
                    use24Hour = preferences.use24HourFormat,
                    showTenths = preferences.showSubSecondTenths,
                    burnInShiftEnabled = preferences.burnInProtectionEnabled,
                    onCycleStyle = onCycleClockStyle,
                    onToggle24Hour = onToggle24Hour,
                    isLockScreenMode = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${clockTime.fullDateFormatted.uppercase()} • DRAIN $currentDrainFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom Lock Screen Quick Actions & Tactile Unlock Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left quick action: Dialer / Emergency Call
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF070D0A))
                            .border(1.dp, dimAccent, CircleShape)
                            .clickable(onClick = onEmergencyDialer)
                            .testTag("lock_screen_dialer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Open Phone Dialer",
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Center Unlock Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .height(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(dimAccent.copy(alpha = 0.25f))
                            .border(1.5.dp, accent, RoundedCornerShape(27.dp))
                            .clickable(onClick = onUnlockToHome)
                            .testTag("unlock_to_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Unlock to Home Screen",
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UNLOCK TO LAUNCHER",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    // Right quick action: Flashlight
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (systemSettings.isFlashlightOn) accent.copy(alpha = 0.25f)
                                else Color(0xFF070D0A)
                            )
                            .border(
                                1.dp,
                                if (systemSettings.isFlashlightOn) accent else dimAccent,
                                CircleShape
                            )
                            .clickable(onClick = onToggleFlashlight)
                            .testTag("lock_screen_torch_button")
                    ) {
                        Icon(
                            imageVector = if (systemSettings.isFlashlightOn) {
                                Icons.Filled.FlashlightOn
                            } else {
                                Icons.Filled.FlashlightOff
                            },
                            contentDescription = "Toggle Torch",
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
