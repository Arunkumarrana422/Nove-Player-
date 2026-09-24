package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserSettings
import com.example.domain.model.ThemePreference
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onDefaultSpeedChange: (Float) -> Unit,
    onDoubleTapSeekChange: (Int) -> Unit,
    onGesturesToggle: (Boolean) -> Unit,
    onSwipeBrightnessToggle: (Boolean) -> Unit,
    onSwipeVolumeToggle: (Boolean) -> Unit,
    onSwipeSeekToggle: (Boolean) -> Unit,
    onBackgroundAudioToggle: (Boolean) -> Unit,
    onHwDecoderToggle: (Boolean) -> Unit,
    onSaveHistoryToggle: (Boolean) -> Unit,
    onRescanLibrary: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearSearchHistory: () -> Unit
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var seekMenuExpanded by remember { mutableStateOf(false) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Preferences & Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APPEARANCE
            item {
                SettingsSection(title = "APPEARANCE") {
                    SettingsClickableRow(
                        icon = Icons.Default.Palette,
                        title = "Theme Mode",
                        subtitle = when (settings.theme) {
                            ThemePreference.SYSTEM -> "Follow System (Default)"
                            ThemePreference.DARK -> "Dark Theme"
                            ThemePreference.LIGHT -> "Light Theme"
                        },
                        onClick = { themeMenuExpanded = true }
                    ) {
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false }
                        ) {
                            ThemePreference.values().forEach { pref ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (pref) {
                                                ThemePreference.SYSTEM -> "System Default"
                                                ThemePreference.DARK -> "Dark Theme"
                                                ThemePreference.LIGHT -> "Light Theme"
                                            }
                                        )
                                    },
                                    onClick = {
                                        themeMenuExpanded = false
                                        onThemeChange(pref)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // PLAYBACK CONTROLS & GESTURES
            item {
                SettingsSection(title = "PLAYBACK & GESTURES") {
                    SettingsClickableRow(
                        icon = Icons.Default.FastForward,
                        title = "Double Tap Seek Duration",
                        subtitle = "${settings.doubleTapSeekSeconds} seconds",
                        onClick = { seekMenuExpanded = true }
                    ) {
                        DropdownMenu(
                            expanded = seekMenuExpanded,
                            onDismissRequest = { seekMenuExpanded = false }
                        ) {
                            listOf(5, 10, 15, 30).forEach { sec ->
                                DropdownMenuItem(
                                    text = { Text("$sec seconds") },
                                    onClick = {
                                        seekMenuExpanded = false
                                        onDoubleTapSeekChange(sec)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsClickableRow(
                        icon = Icons.Default.Speed,
                        title = "Default Playback Speed",
                        subtitle = "${settings.defaultSpeed}x",
                        onClick = { speedMenuExpanded = true }
                    ) {
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = { Text("${spd}x") },
                                    onClick = {
                                        speedMenuExpanded = false
                                        onDefaultSpeedChange(spd)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsSwitchRow(
                        icon = Icons.Default.Gesture,
                        title = "Player Gestures",
                        subtitle = "Enable touch controls on video viewport",
                        checked = settings.gesturesEnabled,
                        onCheckedChange = onGesturesToggle
                    )

                    if (settings.gesturesEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        SettingsSwitchRow(
                            icon = Icons.Default.BrightnessMedium,
                            title = "Vertical Swipe Brightness",
                            subtitle = "Swipe left side of screen to adjust brightness",
                            checked = settings.swipeBrightnessEnabled,
                            onCheckedChange = onSwipeBrightnessToggle
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        SettingsSwitchRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Vertical Swipe Volume",
                            subtitle = "Swipe right side of screen to adjust volume",
                            checked = settings.swipeVolumeEnabled,
                            onCheckedChange = onSwipeVolumeToggle
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        SettingsSwitchRow(
                            icon = Icons.Default.FastForward,
                            title = "Horizontal Swipe Seek",
                            subtitle = "Swipe horizontally to seek backward or forward",
                            checked = settings.swipeSeekEnabled,
                            onCheckedChange = onSwipeSeekToggle
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsSwitchRow(
                        icon = Icons.Default.Headphones,
                        title = "Background Audio Playback",
                        subtitle = "Keep audio running when switching apps",
                        checked = settings.backgroundAudioEnabled,
                        onCheckedChange = onBackgroundAudioToggle
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsSwitchRow(
                        icon = Icons.Default.Memory,
                        title = "Hardware Decoder (HW+)",
                        subtitle = "Enable hardware video decoder by default",
                        checked = settings.hardwareDecoderEnabled,
                        onCheckedChange = onHwDecoderToggle
                    )
                }
            }

            // PRIVACY & STORAGE
            item {
                SettingsSection(title = "LIBRARY & PRIVACY") {
                    SettingsClickableRow(
                        icon = Icons.Default.Refresh,
                        title = "Rescan Media Library",
                        subtitle = "Scan device storage for newly added videos",
                        onClick = onRescanLibrary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsSwitchRow(
                        icon = Icons.Default.Security,
                        title = "Save Watch History",
                        subtitle = "Keep track of played videos and resume positions",
                        checked = settings.saveHistory,
                        onCheckedChange = onSaveHistoryToggle
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsClickableRow(
                        icon = Icons.Default.DeleteOutline,
                        title = "Clear Watch History",
                        subtitle = "Erase all recent watch history",
                        onClick = onClearWatchHistory
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SettingsClickableRow(
                        icon = Icons.Default.DeleteOutline,
                        title = "Clear Search History",
                        subtitle = "Remove all past searches",
                        onClick = onClearSearchHistory
                    )
                }
            }

            // ABOUT
            item {
                SettingsSection(title = "ABOUT ARVEXA PLAYER") {
                    SettingsClickableRow(
                        icon = Icons.Default.Info,
                        title = "Arvexa Player",
                        subtitle = "Version 1.0.1 (Hardware Accelerated Media Engine)",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("Arvexa Player v1.0.1", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("• Hardware Accelerated Video Decoding (HW+ / SW)")
                    Text("• Arvexa Fluid Gestures (Volume, Brightness, Seek, 2x Speed)")
                    Text("• Multi-track Audio and Subtitle sync (.srt / .vtt)")
                    Text("• Custom Playlists and Folder Exploration")
                    Text("• HTTP, HTTPS, and HLS Streaming Support")
                    Text("• Picture-in-Picture & Background Playback")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    dropdown: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NovaAccent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        dropdown()
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NovaAccent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NovaPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
