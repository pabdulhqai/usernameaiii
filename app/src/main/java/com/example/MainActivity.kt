package com.example

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LauncherPreferencesRepository
import com.example.data.SystemControlManager
import com.example.model.DefaultEssentialRole
import com.example.model.EssentialAppSlot
import com.example.model.LauncherDestination
import com.example.model.RingerProfile
import com.example.ui.components.QuickCalculatorDialog
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreenView
import com.example.ui.screens.SystemCoreScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EcoLauncherViewModel

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: LauncherPreferencesRepository
    private lateinit var systemControlManager: SystemControlManager
    private var viewModelInstance: EcoLauncherViewModel? = null

    private val screenLockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val locked = km?.isKeyguardLocked == true
            when (action) {
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_SCREEN_ON -> {
                    viewModelInstance?.onScreenWakeOrLocked(isDeviceLocked = locked)
                }
                Intent.ACTION_USER_PRESENT -> {
                    viewModelInstance?.refreshSystemAndBatteryState()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenWindowFlags()
        enableEdgeToEdge()

        preferencesRepository = LauncherPreferencesRepository(applicationContext)
        systemControlManager = SystemControlManager(applicationContext).apply {
            windowBrightnessUpdater = { brightnessFraction ->
                runOnUiThread {
                    val params = window.attributes
                    params.screenBrightness = brightnessFraction
                    window.attributes = params
                }
            }
        }

        registerScreenStateReceiver()

        setContent {
            val vm: EcoLauncherViewModel = viewModel(
                factory = EcoLauncherViewModel.Factory(
                    preferencesRepository = preferencesRepository,
                    systemControlManager = systemControlManager
                )
            )
            viewModelInstance = vm

            val prefs by vm.userPreferences.collectAsStateWithLifecycle()
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(
                phosphorTheme = prefs.phosphorTheme,
                pureBlackOled = prefs.pureBlackOled
            ) {
                EcoClockLauncherRoot(
                    viewModel = vm,
                    uiState = uiState,
                    preferences = prefs
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km?.isKeyguardLocked == true) {
            viewModelInstance?.onScreenWakeOrLocked(isDeviceLocked = true)
        } else {
            viewModelInstance?.refreshSystemAndBatteryState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenLockReceiver)
        } catch (_: Exception) {
        }
    }

    private fun configureLockScreenWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenLockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenLockReceiver, filter)
        }
    }
}

@Composable
fun EcoClockLauncherRoot(
    viewModel: EcoLauncherViewModel,
    uiState: com.example.viewmodel.EcoLauncherUiState,
    preferences: com.example.data.LauncherUserPreferences
) {
    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer
    val isLockScreenMode = uiState.currentDestination == LauncherDestination.LOCK_SCREEN

    // Prevent accidental back button exit when acting as the Home Launcher
    BackHandler(enabled = uiState.currentDestination != LauncherDestination.HOME) {
        viewModel.navigateTo(LauncherDestination.HOME)
    }

    if (uiState.showQuickCalculatorDialog) {
        QuickCalculatorDialog(onDismiss = viewModel::dismissQuickCalculator)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isLockScreenMode) {
                NavigationBar(
                    containerColor = Color(0xFF040806),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier
                        .border(width = 1.dp, color = dimAccent.copy(alpha = 0.6f))
                        .testTag("bottom_navigation_bar")
                ) {
                    val navItems = listOf(
                        Triple(
                            LauncherDestination.HOME,
                            stringResource(R.string.home_tab),
                            Icons.Filled.Home to Icons.Outlined.Home
                        ),
                        Triple(
                            LauncherDestination.SYSTEM_CORE,
                            stringResource(R.string.system_tab),
                            Icons.Filled.Tune to Icons.Outlined.Tune
                        ),
                        Triple(
                            LauncherDestination.LOCK_SCREEN,
                            stringResource(R.string.lock_tab),
                            Icons.Filled.Lock to Icons.Outlined.Lock
                        ),
                        Triple(
                            LauncherDestination.APP_DRAWER,
                            stringResource(R.string.apps_tab),
                            Icons.Filled.Apps to Icons.Outlined.Apps
                        )
                    )

                    navItems.forEach { (destination, label, icons) ->
                        val selected = uiState.currentDestination == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) icons.first else icons.second,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    text = label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = accent,
                                indicatorColor = accent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${destination.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dismissible Status Feedback Banner for System Settings Mutations
            AnimatedVisibility(visible = uiState.statusBannerMessage != null && !isLockScreenMode) {
                uiState.statusBannerMessage?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(dimAccent.copy(alpha = 0.4f))
                            .border(1.dp, accent, RoundedCornerShape(10.dp))
                            .clickable { viewModel.clearStatusBanner() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("status_feedback_banner"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss notification",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val drainStr = viewModel.formatCurrentDrain(uiState.batteryTelemetry.currentNowMicroAmps)

                when (uiState.currentDestination) {
                    LauncherDestination.HOME -> HomeScreen(
                        clockTime = uiState.clockTime,
                        batteryTelemetry = uiState.batteryTelemetry,
                        systemSettings = uiState.systemSettings,
                        preferences = preferences,
                        currentDrainFormatted = drainStr,
                        onToggleUltraPowerSave = viewModel::toggleUltraPowerSaveMode,
                        onCycleClockStyle = viewModel::cycleClockFaceStyle,
                        onToggle24Hour = {
                            viewModel.setUse24HourFormat(!preferences.use24HourFormat)
                        },
                        onQuickBrightnessStep = {
                            val current = uiState.systemSettings.systemBrightness
                            val next = when {
                                current < 30 -> 64     // 25%
                                current < 100 -> 153   // 60%
                                current < 200 -> 255   // 100%
                                else -> 18             // 7% Ultra-Save
                            }
                            viewModel.updateSystemBrightness(next)
                        },
                        onQuickTimeoutStep = {
                            val current = uiState.systemSettings.screenOffTimeoutMs
                            val next = when {
                                current <= 15_000 -> 30_000
                                current <= 30_000 -> 60_000
                                current <= 60_000 -> 300_000
                                else -> 15_000
                            }
                            viewModel.updateScreenTimeout(next)
                        },
                        onCycleRinger = {
                            val next = when (uiState.systemSettings.ringerProfile) {
                                RingerProfile.NORMAL -> RingerProfile.VIBRATE
                                RingerProfile.VIBRATE -> RingerProfile.SILENT
                                RingerProfile.SILENT -> RingerProfile.NORMAL
                            }
                            viewModel.updateRingerProfile(next)
                        },
                        onToggleFlashlight = viewModel::toggleFlashlight,
                        onOpenWifiPanel = viewModel::openWifiPanel,
                        onEnterLockScreenStandby = {
                            viewModel.navigateTo(LauncherDestination.LOCK_SCREEN)
                        },
                        onLaunchSlot = viewModel::launchEssentialSlot,
                        onAssignSlot = viewModel::openSlotAssignmentPicker,
                        onSetDefaultLauncher = viewModel::openDefaultHomeRole,
                        onOpenWriteSettingsPermission = viewModel::openWriteSettingsPermission
                    )

                    LauncherDestination.SYSTEM_CORE -> SystemCoreScreen(
                        systemSettings = uiState.systemSettings,
                        preferences = preferences,
                        onBrightnessChange = viewModel::updateSystemBrightness,
                        onAutoBrightnessChange = viewModel::updateAutoBrightness,
                        onScreenTimeoutChange = viewModel::updateScreenTimeout,
                        onHapticFeedbackChange = viewModel::updateHapticFeedback,
                        onSoundEffectsChange = viewModel::updateSoundEffects,
                        onMediaVolumeChange = viewModel::updateMediaVolume,
                        onRingerProfileChange = viewModel::updateRingerProfile,
                        onSelectPhosphorTheme = viewModel::setPhosphorTheme,
                        onSelectClockStyle = viewModel::setClockFaceStyle,
                        onToggle24Hour = viewModel::setUse24HourFormat,
                        onToggleTenths = viewModel::setShowSubSecondTenths,
                        onToggleBurnInProtection = viewModel::setBurnInProtection,
                        onToggleAutoShowOnLock = viewModel::setAutoShowOnLockScreen,
                        onOpenWriteSettingsPermission = viewModel::openWriteSettingsPermission,
                        onOpenDndPermission = viewModel::openDndPolicyPermission,
                        onOpenSystemAction = viewModel::openSystemAction,
                        onRefreshSettings = viewModel::refreshSystemAndBatteryState
                    )

                    LauncherDestination.LOCK_SCREEN -> LockScreenView(
                        clockTime = uiState.clockTime,
                        batteryTelemetry = uiState.batteryTelemetry,
                        systemSettings = uiState.systemSettings,
                        preferences = preferences,
                        currentDrainFormatted = drainStr,
                        onUnlockToHome = {
                            viewModel.navigateTo(LauncherDestination.HOME)
                        },
                        onCycleClockStyle = viewModel::cycleClockFaceStyle,
                        onToggle24Hour = {
                            viewModel.setUse24HourFormat(!preferences.use24HourFormat)
                        },
                        onToggleFlashlight = viewModel::toggleFlashlight,
                        onEmergencyDialer = {
                            viewModel.launchEssentialSlot(
                                EssentialAppSlot(
                                    slotIndex = 0,
                                    role = DefaultEssentialRole.DIALER
                                )
                            )
                        }
                    )

                    LauncherDestination.APP_DRAWER -> AppDrawerScreen(
                        installedApps = uiState.installedApps,
                        searchQuery = uiState.appSearchQuery,
                        slotSelectionTarget = uiState.slotSelectionTarget,
                        onSearchQueryChange = viewModel::updateAppSearchQuery,
                        onLaunchApp = viewModel::launchInstalledApp,
                        onAssignAppToSlot = viewModel::assignAppToSlot,
                        onResetSlotToDefault = viewModel::resetSlotToDefault,
                        onCancelSlotAssignment = viewModel::cancelSlotAssignment,
                        onBackToHome = {
                            viewModel.navigateTo(LauncherDestination.HOME)
                        }
                    )
                }
            }
        }
    }
}
