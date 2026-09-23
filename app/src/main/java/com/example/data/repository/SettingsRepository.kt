package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.SortOption
import com.example.domain.model.ThemePreference
import com.example.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_player_settings")

data class UserSettings(
    val theme: ThemePreference = ThemePreference.DARK,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val viewMode: ViewMode = ViewMode.LIST,
    val defaultSpeed: Float = 1.0f,
    val doubleTapSeekSeconds: Int = 10,
    val gesturesEnabled: Boolean = true,
    val swipeBrightnessEnabled: Boolean = true,
    val swipeVolumeEnabled: Boolean = true,
    val swipeSeekEnabled: Boolean = true,
    val defaultAspectRatio: AspectRatioMode = AspectRatioMode.FIT,
    val subtitleFontSize: Int = 18,
    val subtitleTextColor: String = "#FFFFFF",
    val subtitleBgColor: String = "#80000000",
    val autoHideControlsSeconds: Int = 4,
    val resumePlayback: Boolean = true,
    val backgroundAudioEnabled: Boolean = true,
    val hardwareDecoderEnabled: Boolean = true,
    val saveHistory: Boolean = true,
    val onboardingCompleted: Boolean = false
)

class SettingsRepository(private val context: Context) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_preference")
        private val KEY_SORT = stringPreferencesKey("sort_option")
        private val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
        private val KEY_DEFAULT_SPEED = floatPreferencesKey("default_speed")
        private val KEY_DOUBLE_TAP_SEEK = intPreferencesKey("double_tap_seek")
        private val KEY_GESTURES = booleanPreferencesKey("gestures_enabled")
        private val KEY_SWIPE_BRIGHTNESS = booleanPreferencesKey("swipe_brightness")
        private val KEY_SWIPE_VOLUME = booleanPreferencesKey("swipe_volume")
        private val KEY_SWIPE_SEEK = booleanPreferencesKey("swipe_seek")
        private val KEY_ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        private val KEY_SUBTITLE_FONT_SIZE = intPreferencesKey("subtitle_font_size")
        private val KEY_SUBTITLE_TEXT_COLOR = stringPreferencesKey("subtitle_text_color")
        private val KEY_SUBTITLE_BG_COLOR = stringPreferencesKey("subtitle_bg_color")
        private val KEY_AUTO_HIDE_CONTROLS = intPreferencesKey("auto_hide_controls")
        private val KEY_RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        private val KEY_BG_AUDIO = booleanPreferencesKey("bg_audio")
        private val KEY_HW_DECODER = booleanPreferencesKey("hw_decoder")
        private val KEY_SAVE_HISTORY = booleanPreferencesKey("save_history")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            theme = try {
                ThemePreference.valueOf(prefs[KEY_THEME] ?: ThemePreference.DARK.name)
            } catch (e: Exception) {
                ThemePreference.DARK
            },
            sortOption = try {
                SortOption.valueOf(prefs[KEY_SORT] ?: SortOption.DATE_DESC.name)
            } catch (e: Exception) {
                SortOption.DATE_DESC
            },
            viewMode = try {
                ViewMode.valueOf(prefs[KEY_VIEW_MODE] ?: ViewMode.LIST.name)
            } catch (e: Exception) {
                ViewMode.LIST
            },
            defaultSpeed = prefs[KEY_DEFAULT_SPEED] ?: 1.0f,
            doubleTapSeekSeconds = prefs[KEY_DOUBLE_TAP_SEEK] ?: 10,
            gesturesEnabled = prefs[KEY_GESTURES] ?: true,
            swipeBrightnessEnabled = prefs[KEY_SWIPE_BRIGHTNESS] ?: true,
            swipeVolumeEnabled = prefs[KEY_SWIPE_VOLUME] ?: true,
            swipeSeekEnabled = prefs[KEY_SWIPE_SEEK] ?: true,
            defaultAspectRatio = try {
                AspectRatioMode.valueOf(prefs[KEY_ASPECT_RATIO] ?: AspectRatioMode.FIT.name)
            } catch (e: Exception) {
                AspectRatioMode.FIT
            },
            subtitleFontSize = prefs[KEY_SUBTITLE_FONT_SIZE] ?: 18,
            subtitleTextColor = prefs[KEY_SUBTITLE_TEXT_COLOR] ?: "#FFFFFF",
            subtitleBgColor = prefs[KEY_SUBTITLE_BG_COLOR] ?: "#80000000",
            autoHideControlsSeconds = prefs[KEY_AUTO_HIDE_CONTROLS] ?: 4,
            resumePlayback = prefs[KEY_RESUME_PLAYBACK] ?: true,
            backgroundAudioEnabled = prefs[KEY_BG_AUDIO] ?: true,
            hardwareDecoderEnabled = prefs[KEY_HW_DECODER] ?: true,
            saveHistory = prefs[KEY_SAVE_HISTORY] ?: true,
            onboardingCompleted = prefs[KEY_ONBOARDING] ?: false
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setSortOption(sort: SortOption) {
        context.dataStore.edit { it[KEY_SORT] = sort.name }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[KEY_VIEW_MODE] = mode.name }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[KEY_DEFAULT_SPEED] = speed }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.dataStore.edit { it[KEY_DOUBLE_TAP_SEEK] = seconds }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GESTURES] = enabled }
    }

    suspend fun setSwipeBrightness(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SWIPE_BRIGHTNESS] = enabled }
    }

    suspend fun setSwipeVolume(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SWIPE_VOLUME] = enabled }
    }

    suspend fun setSwipeSeek(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SWIPE_SEEK] = enabled }
    }

    suspend fun setDefaultAspectRatio(aspectRatio: AspectRatioMode) {
        context.dataStore.edit { it[KEY_ASPECT_RATIO] = aspectRatio.name }
    }

    suspend fun setSubtitleFontSize(size: Int) {
        context.dataStore.edit { it[KEY_SUBTITLE_FONT_SIZE] = size }
    }

    suspend fun setSubtitleColors(textColor: String, bgColor: String) {
        context.dataStore.edit {
            it[KEY_SUBTITLE_TEXT_COLOR] = textColor
            it[KEY_SUBTITLE_BG_COLOR] = bgColor
        }
    }

    suspend fun setAutoHideSeconds(seconds: Int) {
        context.dataStore.edit { it[KEY_AUTO_HIDE_CONTROLS] = seconds }
    }

    suspend fun setResumePlayback(resume: Boolean) {
        context.dataStore.edit { it[KEY_RESUME_PLAYBACK] = resume }
    }

    suspend fun setBackgroundAudio(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BG_AUDIO] = enabled }
    }

    suspend fun setHardwareDecoder(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HW_DECODER] = enabled }
    }

    suspend fun setSaveHistory(save: Boolean) {
        context.dataStore.edit { it[KEY_SAVE_HISTORY] = save }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = completed }
    }
}
