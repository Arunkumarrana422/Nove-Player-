package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SubtitleTrack
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsBottomSheet(
    tracks: List<SubtitleTrack>,
    selectedTrack: SubtitleTrack?,
    subtitleDelayMs: Long,
    fontSize: Int,
    textColor: String,
    bgColor: String,
    onSelectTrack: (SubtitleTrack?) -> Unit,
    onLoadExternalSubtitle: (Uri) -> Unit,
    onAdjustDelay: (Long) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onColorChange: (textColor: String, bgColor: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onLoadExternalSubtitle(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("subtitle_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = NovaAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Subtitle Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle Tracks Section
            Text(
                text = "SUBTITLE TRACKS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Off option
            TrackRow(
                title = "Subtitles Off",
                subtitle = "Disable subtitle overlay",
                isSelected = selectedTrack == null,
                icon = Icons.Default.SubtitlesOff,
                onClick = { onSelectTrack(null) }
            )

            tracks.forEach { track ->
                TrackRow(
                    title = track.label,
                    subtitle = if (track.isExternal) "External File (.srt/.vtt)" else "Embedded Track (${track.language})",
                    isSelected = selectedTrack?.id == track.id,
                    icon = Icons.Default.Subtitles,
                    onClick = { onSelectTrack(track) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load External Subtitle File (.srt / .vtt)")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 14.dp))

            // Synchronization / Delay Adjustment
            Text(
                text = "SUBTITLE SYNCHRONIZATION",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Offset: ${if (subtitleDelayMs >= 0) "+" else ""}${subtitleDelayMs} ms",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NovaAccent
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onAdjustDelay(-100L) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("-100ms", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onAdjustDelay(100L) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+100ms", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 14.dp))

            // Font Size
            Text(
                text = "FONT SIZE (${fontSize}sp)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var sliderFontSize by remember(fontSize) { mutableFloatStateOf(fontSize.toFloat()) }
            Slider(
                value = sliderFontSize,
                onValueChange = {
                    sliderFontSize = it
                    onFontSizeChange(it.toInt())
                },
                valueRange = 12f..32f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = NovaAccent,
                    activeTrackColor = NovaAccent
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 14.dp))

            // Style / Color Schemes
            Text(
                text = "SUBTITLE COLOR PRESET",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorPresetChip(
                    name = "Classic White",
                    textColor = "#FFFFFF",
                    bgColor = "#80000000",
                    isSelected = textColor == "#FFFFFF" && bgColor == "#80000000",
                    onClick = { onColorChange("#FFFFFF", "#80000000") },
                    modifier = Modifier.weight(1f)
                )
                ColorPresetChip(
                    name = "Vibrant Yellow",
                    textColor = "#FACC15",
                    bgColor = "#80000000",
                    isSelected = textColor == "#FACC15",
                    onClick = { onColorChange("#FACC15", "#80000000") },
                    modifier = Modifier.weight(1f)
                )
                ColorPresetChip(
                    name = "Cyan Glow",
                    textColor = "#22D3EE",
                    bgColor = "#99000000",
                    isSelected = textColor == "#22D3EE",
                    onClick = { onColorChange("#22D3EE", "#99000000") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun TrackRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) NovaPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) NovaAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) NovaAccent else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = NovaAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ColorPresetChip(
    name: String,
    textColor: String,
    bgColor: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NovaPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) NovaAccent else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(android.graphics.Color.parseColor(textColor)), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
