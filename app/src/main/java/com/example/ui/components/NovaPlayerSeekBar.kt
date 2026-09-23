package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Video
import com.example.ui.theme.NovaAccent

/**
 * Custom modern Video Player Seekbar matching Photo 3 design:
 * - Rounded pill background track
 * - Bright cyan played progress bar
 * - Vertical neon cyan tick indicator / cursor for thumb
 * - Left & Right timestamp display
 */
@Composable
fun NovaPlayerSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val actualProgressFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayProgressFraction = if (isDragging) dragProgressFraction else actualProgressFraction
    val bufferedFraction = (bufferedPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val currentDisplayPosMs = (displayProgressFraction * safeDuration).toLong()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left timestamp
        Text(
            text = Video.formatDuration(currentDisplayPosMs),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Custom Seekbar Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .testTag("nova_player_seekbar")
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekTo((fraction * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgressFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeekTo((dragProgressFraction * safeDuration).toLong())
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgressFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                val width = size.width
                val height = size.height

                val trackHeight = 10.dp.toPx()
                val trackCornerRadius = trackHeight / 2f
                val trackTop = (height - trackHeight) / 2f

                // 1. Inactive Track (Dark Grey pill)
                drawRoundRect(
                    color = Color(0xFF404654),
                    topLeft = Offset(0f, trackTop),
                    size = Size(width, trackHeight),
                    cornerRadius = CornerRadius(trackCornerRadius, trackCornerRadius)
                )

                // 2. Buffered Track
                if (bufferedFraction > 0f) {
                    val bufferedWidth = width * bufferedFraction
                    drawRoundRect(
                        color = Color(0xFF5A6478),
                        topLeft = Offset(0f, trackTop),
                        size = Size(bufferedWidth, trackHeight),
                        cornerRadius = CornerRadius(trackCornerRadius, trackCornerRadius)
                    )
                }

                // 3. Active Progress Track (Bright Neon Cyan)
                val progressWidth = (width * displayProgressFraction).coerceIn(0f, width)
                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(0f, trackTop),
                        size = Size(progressWidth, trackHeight),
                        cornerRadius = CornerRadius(trackCornerRadius, trackCornerRadius)
                    )
                }

                // 4. Subtle buffer indicator dot on unplayed section if applicable
                if (bufferedFraction > displayProgressFraction && bufferedFraction < 0.99f) {
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(width * bufferedFraction, height / 2f)
                    )
                }

                // 5. Thumb: Distinct Vertical Cyan Pill Cursor (Matching Photo 3)
                val cursorWidth = 3.5.dp.toPx()
                val cursorHeight = 22.dp.toPx()
                val cursorLeft = (progressWidth - cursorWidth / 2f).coerceIn(0f, width - cursorWidth)
                val cursorTop = (height - cursorHeight) / 2f

                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(cursorLeft, cursorTop),
                    size = Size(cursorWidth, cursorHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }

        // Right timestamp
        Text(
            text = Video.formatDuration(durationMs),
            color = Color(0xCCFFFFFF),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
