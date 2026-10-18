package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ClockFaceStyle
import com.example.model.DefaultEssentialRole
import com.example.model.EssentialAppSlot
import com.example.model.PhosphorTheme
import com.example.model.SavedNormalProfileSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ecoclock_launcher_prefs"
)

data class LauncherUserPreferences(
    val isUltraPowerSaveActive: Boolean = false,
    val use24HourFormat: Boolean = true,
    val showSubSecondTenths: Boolean = true,
    val clockFaceStyle: ClockFaceStyle = ClockFaceStyle.CHRONO_SPLIT,
    val phosphorTheme: PhosphorTheme = PhosphorTheme.EMERALD,
    val pureBlackOled: Boolean = true,
    val burnInProtectionEnabled: Boolean = true,
    val autoShowOnLockScreen: Boolean = true,
    val essentialSlots: List<EssentialAppSlot> = DefaultEssentialRole.entries.map {
        EssentialAppSlot(slotIndex = it.slotIndex, role = it)
    },
    val savedNormalSnapshot: SavedNormalProfileSnapshot = SavedNormalProfileSnapshot()
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val ULTRA_POWER_SAVE = booleanPreferencesKey("ultra_power_save")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val SHOW_TENTHS = booleanPreferencesKey("show_tenths")
        val CLOCK_FACE_STYLE = stringPreferencesKey("clock_face_style")
        val PHOSPHOR_THEME = stringPreferencesKey("phosphor_theme")
        val PURE_BLACK_OLED = booleanPreferencesKey("pure_black_oled")
        val BURN_IN_PROTECTION = booleanPreferencesKey("burn_in_protection")
        val AUTO_SHOW_ON_LOCK = booleanPreferencesKey("auto_show_on_lock")

        val SNAP_BRIGHTNESS = intPreferencesKey("snap_brightness")
        val SNAP_AUTO_BRIGHTNESS = booleanPreferencesKey("snap_auto_brightness")
        val SNAP_TIMEOUT_MS = intPreferencesKey("snap_timeout_ms")
        val SNAP_HAPTIC = booleanPreferencesKey("snap_haptic")
        val SNAP_SOUND = booleanPreferencesKey("snap_sound")
        val SNAP_MEDIA_VOL = intPreferencesKey("snap_media_vol")

        fun slotPkgKey(index: Int) = stringPreferencesKey("slot_${index}_pkg")
        fun slotActKey(index: Int) = stringPreferencesKey("slot_${index}_act")
        fun slotLabelKey(index: Int) = stringPreferencesKey("slot_${index}_label")
    }

    val preferencesFlow: Flow<LauncherUserPreferences> = context.launcherDataStore.data.map { prefs ->
        val styleName = prefs[Keys.CLOCK_FACE_STYLE] ?: ClockFaceStyle.CHRONO_SPLIT.name
        val style = ClockFaceStyle.entries.firstOrNull { it.name == styleName }
            ?: ClockFaceStyle.CHRONO_SPLIT

        val themeId = prefs[Keys.PHOSPHOR_THEME] ?: PhosphorTheme.EMERALD.id
        val theme = PhosphorTheme.fromId(themeId)

        val slots = DefaultEssentialRole.entries.map { role ->
            val idx = role.slotIndex
            EssentialAppSlot(
                slotIndex = idx,
                role = role,
                customLabel = prefs[Keys.slotLabelKey(idx)],
                assignedPackageName = prefs[Keys.slotPkgKey(idx)],
                assignedActivityName = prefs[Keys.slotActKey(idx)]
            )
        }

        LauncherUserPreferences(
            isUltraPowerSaveActive = prefs[Keys.ULTRA_POWER_SAVE] ?: false,
            use24HourFormat = prefs[Keys.USE_24_HOUR] ?: true,
            showSubSecondTenths = prefs[Keys.SHOW_TENTHS] ?: true,
            clockFaceStyle = style,
            phosphorTheme = theme,
            pureBlackOled = prefs[Keys.PURE_BLACK_OLED] ?: true,
            burnInProtectionEnabled = prefs[Keys.BURN_IN_PROTECTION] ?: true,
            autoShowOnLockScreen = prefs[Keys.AUTO_SHOW_ON_LOCK] ?: true,
            essentialSlots = slots,
            savedNormalSnapshot = SavedNormalProfileSnapshot(
                brightness = prefs[Keys.SNAP_BRIGHTNESS] ?: 140,
                autoBrightness = prefs[Keys.SNAP_AUTO_BRIGHTNESS] ?: false,
                timeoutMs = prefs[Keys.SNAP_TIMEOUT_MS] ?: 60_000,
                hapticEnabled = prefs[Keys.SNAP_HAPTIC] ?: true,
                soundEnabled = prefs[Keys.SNAP_SOUND] ?: true,
                mediaVolumePercent = prefs[Keys.SNAP_MEDIA_VOL] ?: 50
            )
        )
    }

    suspend fun setUltraPowerSave(active: Boolean, snapshotToSave: SavedNormalProfileSnapshot? = null) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.ULTRA_POWER_SAVE] = active
            if (snapshotToSave != null) {
                prefs[Keys.SNAP_BRIGHTNESS] = snapshotToSave.brightness
                prefs[Keys.SNAP_AUTO_BRIGHTNESS] = snapshotToSave.autoBrightness
                prefs[Keys.SNAP_TIMEOUT_MS] = snapshotToSave.timeoutMs
                prefs[Keys.SNAP_HAPTIC] = snapshotToSave.hapticEnabled
                prefs[Keys.SNAP_SOUND] = snapshotToSave.soundEnabled
                prefs[Keys.SNAP_MEDIA_VOL] = snapshotToSave.mediaVolumePercent
            }
        }
    }

    suspend fun setUse24HourFormat(use24Hour: Boolean) {
        context.launcherDataStore.edit { it[Keys.USE_24_HOUR] = use24Hour }
    }

    suspend fun setShowSubSecondTenths(show: Boolean) {
        context.launcherDataStore.edit { it[Keys.SHOW_TENTHS] = show }
    }

    suspend fun setClockFaceStyle(style: ClockFaceStyle) {
        context.launcherDataStore.edit { it[Keys.CLOCK_FACE_STYLE] = style.name }
    }

    suspend fun setPhosphorTheme(theme: PhosphorTheme) {
        context.launcherDataStore.edit { it[Keys.PHOSPHOR_THEME] = theme.id }
    }

    suspend fun setPureBlackOled(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.PURE_BLACK_OLED] = enabled }
    }

    suspend fun setBurnInProtection(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.BURN_IN_PROTECTION] = enabled }
    }

    suspend fun setAutoShowOnLockScreen(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.AUTO_SHOW_ON_LOCK] = enabled }
    }

    suspend fun assignAppToSlot(slotIndex: Int, label: String, packageName: String, activityName: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.slotLabelKey(slotIndex)] = label
            prefs[Keys.slotPkgKey(slotIndex)] = packageName
            prefs[Keys.slotActKey(slotIndex)] = activityName
        }
    }

    suspend fun resetSlotToDefault(slotIndex: Int) {
        context.launcherDataStore.edit { prefs ->
            prefs.remove(Keys.slotLabelKey(slotIndex))
            prefs.remove(Keys.slotPkgKey(slotIndex))
            prefs.remove(Keys.slotActKey(slotIndex))
        }
    }
}
