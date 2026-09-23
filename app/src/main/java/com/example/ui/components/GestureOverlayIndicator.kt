package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Video
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary

sealed class GestureHudState {
    data object None : GestureHudState()
    data class Volume(val fraction: Float) : GestureHudState()
    data class Brightness(val fraction: Float) : GestureHudState()
    data class Seek(val targetPosMs: Long, val deltaMs: Long, val totalDurationMs: Long) : GestureHudState()
    data class DoubleTapSeek(val isForward: Boolean, val deltaSeconds: Int) : GestureHudState()
    data class SpeedBoost(val speed: Float) : GestureHudState()
}

@Composable
fun GestureOverlayIndicator(
    hudState: GestureHudState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = hudState !is GestureHudState.None,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC0B1020))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            when (hudState) {
                is GestureHudState.Volume -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (hudState.fraction <= 0.05f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = NovaAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Volume: ${(hudState.fraction * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { hudState.fraction },
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NovaAccent,
                            trackColor = Color(0x40FFFFFF)
                        )
                    }
                }
                is GestureHudState.Brightness -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (hudState.fraction < 0.5f) Icons.Default.BrightnessLow else Icons.Default.BrightnessMedium,
                            contentDescription = "Brightness",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Brightness: ${(hudState.fraction * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { hudState.fraction },
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFF59E0B),
                            trackColor = Color(0x40FFFFFF)
                        )
                    }
                }
                is GestureHudState.Seek -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (hudState.deltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = "Seek",
                            tint = NovaAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val sign = if (hudState.deltaMs >= 0) "+" else ""
                        Text(
                            text = "$sign${hudState.deltaMs / 1000}s",
                            color = NovaAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${Video.formatDuration(hudState.targetPosMs)} / ${Video.formatDuration(hudState.totalDurationMs)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is GestureHudState.DoubleTapSeek -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (hudState.isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = NovaAccent,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (hudState.isForward) "+" else "-"}${hudState.deltaSeconds} seconds",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                is GestureHudState.SpeedBoost -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = NovaAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${hudState.speed}X SPEED",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
                GestureHudState.None -> {}
            }
        }
    }
}
