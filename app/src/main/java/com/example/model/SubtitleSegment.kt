package com.example.model

data class SubtitleSegment(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
) {
    val durationMs: Long get() = (endTimeMs - startTimeMs).coerceAtLeast(0)

    fun formattedStartTime(): String = formatSrtTimestamp(startTimeMs)
    fun formattedEndTime(): String = formatSrtTimestamp(endTimeMs)

    fun toSrtBlock(): String {
        return "$index\n${formattedStartTime()} --> ${formattedEndTime()}\n${text.trim()}\n"
    }

    companion object {
        fun formatSrtTimestamp(ms: Long): String {
            val totalSeconds = ms / 1000
            val millis = ms % 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
        }

        fun parseSrt(srtContent: String): List<SubtitleSegment> {
            val segments = mutableListOf<SubtitleSegment>()
            val blocks = srtContent.trim().split(Regex("\n\r?\n\r?"))
            for (block in blocks) {
                val lines = block.lines().filter { it.isNotBlank() }
                if (lines.size >= 2) {
                    val index = lines[0].trim().toIntOrNull() ?: (segments.size + 1)
                    val timeLine = lines[1]
                    val timeParts = timeLine.split("-->")
                    if (timeParts.size == 2) {
                        val startMs = parseTimestampToMs(timeParts[0].trim())
                        val endMs = parseTimestampToMs(timeParts[1].trim())
                        val text = lines.drop(2).joinToString("\n")
                        segments.add(SubtitleSegment(index, startMs, endMs, text))
                    }
                }
            }
            return segments
        }

        private fun parseTimestampToMs(timestamp: String): Long {
            try {
                val normalized = timestamp.replace(',', '.')
                val parts = normalized.split(":")
                if (parts.size == 3) {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val secParts = parts[2].split(".")
                    val seconds = secParts[0].toLong()
                    val millis = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
                    return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
                }
            } catch (_: Exception) {
            }
            return 0L
        }

        fun toFullSrt(segments: List<SubtitleSegment>): String {
            return segments.joinToString(separator = "\n") { it.toSrtBlock() }
        }

        fun toPlainText(segments: List<SubtitleSegment>): String {
            return segments.joinToString(separator = "\n") { it.text.trim() }
        }
    }
}
