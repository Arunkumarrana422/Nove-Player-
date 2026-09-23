package com.example.player

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

object SubtitleParser {
    suspend fun parseFromUri(context: Context, uri: Uri): List<SubtitleCue> = withContext(Dispatchers.IO) {
        val cues = mutableListOf<SubtitleCue>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val lines = reader.readLines()
                    cues.addAll(parseLines(lines))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cues
    }

    fun parseLines(lines: List<String>): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val times = line.split("-->")
                if (times.size == 2) {
                    val startMs = parseTimestamp(times[0].trim())
                    val endMs = parseTimestamp(times[1].trim().split(" ")[0])

                    val textBuilder = StringBuilder()
                    i++
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(lines[i].trim().replace(Regex("<[^>]*>"), ""))
                        i++
                    }

                    if (startMs >= 0 && endMs > startMs) {
                        cues.add(
                            SubtitleCue(
                                startTimeMs = startMs,
                                endTimeMs = endMs,
                                text = textBuilder.toString()
                            )
                        )
                    }
                }
            }
            i++
        }
        return cues
    }

    private fun parseTimestamp(timeStr: String): Long {
        try {
            // Supports "00:01:23,456" or "00:01:23.456" or "01:23.456"
            val clean = timeStr.replace(',', '.')
            val parts = clean.split(":")
            return when (parts.size) {
                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val secMillis = parts[2].split(".")
                    val seconds = secMillis[0].toLong()
                    val millis = if (secMillis.size > 1) secMillis[1].padEnd(3, '0').take(3).toLong() else 0L
                    (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
                }
                2 -> {
                    val minutes = parts[0].toLong()
                    val secMillis = parts[1].split(".")
                    val seconds = secMillis[0].toLong()
                    val millis = if (secMillis.size > 1) secMillis[1].padEnd(3, '0').take(3).toLong() else 0L
                    (minutes * 60 + seconds) * 1000 + millis
                }
                else -> -1L
            }
        } catch (e: Exception) {
            return -1L
        }
    }

    fun getActiveCue(cues: List<SubtitleCue>, currentPositionMs: Long, delayMs: Long = 0L): String? {
        val effectivePosition = currentPositionMs - delayMs
        return cues.firstOrNull { it.startTimeMs <= effectivePosition && effectivePosition <= it.endTimeMs }?.text
    }
}
