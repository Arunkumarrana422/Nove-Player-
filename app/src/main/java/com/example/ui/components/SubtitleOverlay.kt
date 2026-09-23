package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubtitleOverlay(
    subtitleText: String?,
    fontSizeSp: Int = 18,
    textColorHex: String = "#FFFFFF",
    bgColorHex: String = "#80000000",
    modifier: Modifier = Modifier
) {
    val textColor = try {
        Color(android.graphics.Color.parseColor(textColorHex))
    } catch (e: Exception) {
        Color.White
    }

    val bgColor = try {
        Color(android.graphics.Color.parseColor(bgColorHex))
    } catch (e: Exception) {
        Color(0x80000000)
    }

    AnimatedVisibility(
        visible = !subtitleText.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        subtitleText?.let { text ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = text,
                        style = TextStyle(
                            color = textColor,
                            fontSize = fontSizeSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black,
                                blurRadius = 4f
                            )
                        )
                    )
                }
            }
        }
    }
}
