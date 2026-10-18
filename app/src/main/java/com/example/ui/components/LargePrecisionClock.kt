package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClockFaceStyle
import com.example.model.ClockTimeState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LargePrecisionClock(
    clockTime: ClockTimeState,
    clockStyle: ClockFaceStyle,
    use24Hour: Boolean,
    showTenths: Boolean,
    burnInShiftEnabled: Boolean,
    onCycleStyle: () -> Unit,
    onToggle24Hour: () -> Unit,
    modifier: Modifier = Modifier,
    isLockScreenMode: Boolean = false
) {
    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer
    val shiftX = if (burnInShiftEnabled) clockTime.pixelShiftX else 0
    val shiftY = if (burnInShiftEnabled) clockTime.pixelShiftY else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shiftX.dp.roundToPx(), shiftY.dp.roundToPx()) }
            .testTag("large_precision_clock"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top metadata & format switcher bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A110E))
                    .border(1.dp, dimAccent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggle24Hour)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("clock_format_toggle")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Toggle 12/24 hour format",
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${clockTime.dateFormatted} • ${clockTime.amPm}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!isLockScreenMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A110E))
                        .border(1.dp, dimAccent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCycleStyle)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("clock_style_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "Switch clock style",
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = clockStyle.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Clock Display Area (Hours, Minutes, Seconds)
        AnimatedContent(
            targetState = clockStyle,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ClockFaceStyleTransition"
        ) { style ->
            when (style) {
                ClockFaceStyle.CHRONO_SPLIT -> ChronoSplitFace(
                    clockTime = clockTime,
                    showTenths = showTenths,
                    accent = accent,
                    dimAccent = dimAccent
                )
                ClockFaceStyle.ORBITAL_RING -> OrbitalRingFace(
                    clockTime = clockTime,
                    showTenths = showTenths,
                    accent = accent,
                    dimAccent = dimAccent
                )
                ClockFaceStyle.VERTICAL_STACK -> VerticalStackFace(
                    clockTime = clockTime,
                    showTenths = showTenths,
                    accent = accent,
                    dimAccent = dimAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-telemetry footer: System Uptime & Next Alarm
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "UPTIME ${clockTime.uptimeFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = clockTime.nextAlarmFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChronoSplitFace(
    clockTime: ClockTimeState,
    showTenths: Boolean,
    accent: Color,
    dimAccent: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, dimAccent.copy(alpha = 0.85f), RoundedCornerShape(20.dp)),
        color = Color(0xFF030705),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Giant Hours : Minutes
                Text(
                    text = "${clockTime.hoursFormatted}:${clockTime.minutesFormatted}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 68.sp,
                    lineHeight = 68.sp,
                    letterSpacing = (-2.5).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("clock_hours_minutes_text")
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Prominent Boxed Seconds (:SS + optional .d tenths)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(dimAccent.copy(alpha = 0.28f))
                        .border(1.5.dp, accent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SEC",
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = accent.copy(alpha = 0.9f)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = clockTime.secondsFormatted,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            lineHeight = 32.sp,
                            color = accent,
                            modifier = Modifier.testTag("clock_seconds_text")
                        )
                        if (showTenths) {
                            Text(
                                text = ".${clockTime.tenthsFormatted}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = accent.copy(alpha = 0.75f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 60-Second Precision Segmented Tick Bar
            SixtySecondSegmentBar(
                currentSecond = clockTime.secondsInt,
                millisInSecond = clockTime.millisInSecond,
                accent = accent,
                dimAccent = dimAccent
            )
        }
    }
}

@Composable
private fun SixtySecondSegmentBar(
    currentSecond: Int,
    millisInSecond: Int,
    accent: Color,
    dimAccent: Color
) {
    val smoothProgress = (currentSecond + (millisInSecond / 1000f)) / 60f

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            val totalTicks = 60
            val gap = 2.dp.toPx()
            val availableWidth = size.width - (gap * (totalTicks - 1))
            val tickWidth = (availableWidth / totalTicks).coerceAtLeast(1.5f)

            for (i in 0 until totalTicks) {
                val x = i * (tickWidth + gap)
                val isMajor = i % 5 == 0
                val topY = if (isMajor) 0f else size.height * 0.25f
                val tickColor = when {
                    i < currentSecond -> accent
                    i == currentSecond -> Color.White
                    else -> dimAccent.copy(alpha = 0.38f)
                }
                drawLine(
                    color = tickColor,
                    start = Offset(x + tickWidth / 2f, topY),
                    end = Offset(x + tickWidth / 2f, size.height),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00s", style = MaterialTheme.typography.labelSmall, color = accent.copy(alpha = 0.7f))
            Text(
                text = "${(smoothProgress * 100).roundToInt()}% MINUTE CYCLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("60s", style = MaterialTheme.typography.labelSmall, color = accent.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun OrbitalRingFace(
    clockTime: ClockTimeState,
    showTenths: Boolean,
    accent: Color,
    dimAccent: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.size(230.dp)) {
            val radius = size.minDimension / 2f - 10.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Subtle base track
            drawCircle(
                color = dimAccent.copy(alpha = 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 60 perimeter radial second ticks
            for (sec in 0 until 60) {
                val angleDeg = sec * 6f - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = sec % 5 == 0
                val outerR = radius - 5.dp.toPx()
                val innerR = outerR - if (isMajor) 11.dp.toPx() else 6.dp.toPx()

                val start = Offset(
                    x = center.x + (innerR * cos(angleRad)).toFloat(),
                    y = center.y + (innerR * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = center.x + (outerR * cos(angleRad)).toFloat(),
                    y = center.y + (outerR * sin(angleRad)).toFloat()
                )

                val tickColor = when {
                    sec == clockTime.secondsInt -> Color.White
                    sec < clockTime.secondsInt -> accent
                    else -> dimAccent.copy(alpha = 0.45f)
                }

                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Smooth sweeping arc for current second
            val sweepAngle = ((clockTime.secondsInt + clockTime.millisInSecond / 1000f) / 60f) * 360f
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${clockTime.hoursFormatted}:${clockTime.minutesFormatted}",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                letterSpacing = (-1.5).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("clock_hours_minutes_text")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(dimAccent.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ":${clockTime.secondsFormatted}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = accent,
                    modifier = Modifier.testTag("clock_seconds_text")
                )
                if (showTenths) {
                    Text(
                        text = ".${clockTime.tenthsFormatted}s",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 13.sp,
                        color = accent.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalStackFace(
    clockTime: ClockTimeState,
    showTenths: Boolean,
    accent: Color,
    dimAccent: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, dimAccent.copy(alpha = 0.85f), RoundedCornerShape(20.dp)),
        color = Color(0xFF030705),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePillarCard(label = "HOURS", value = clockTime.hoursFormatted, color = Color.White)
            Text(
                text = ":",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = accent.copy(alpha = 0.7f)
            )
            TimePillarCard(label = "MINUTES", value = clockTime.minutesFormatted, color = Color.White)
            Text(
                text = ":",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = accent
            )
            TimePillarCard(
                label = if (showTenths) "SEC .${clockTime.tenthsFormatted}" else "SECONDS",
                value = clockTime.secondsFormatted,
                color = accent,
                isHighlighted = true,
                testTag = "clock_seconds_text"
            )
        }
    }
}

@Composable
private fun TimePillarCard(
    label: String,
    value: String,
    color: Color,
    isHighlighted: Boolean = false,
    testTag: String? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isHighlighted) accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = if (isHighlighted) 1.5.dp else 0.dp,
                color = if (isHighlighted) accent else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHighlighted) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 46.sp,
            lineHeight = 50.sp,
            color = color,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
