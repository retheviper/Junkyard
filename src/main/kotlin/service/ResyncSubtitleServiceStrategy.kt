package service

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import org.koin.core.component.KoinComponent

enum class ResyncSubtitleType {
    SMI,
    SRT;

    companion object {
        fun fromString(value: String): ResyncSubtitleType {
            return valueOf(value.uppercase())
        }
    }
}

class ResyncSubtitleServiceStrategyFactory : KoinComponent {
    private val strategies: Set<ResyncSubtitleServiceStrategy> = setOf(
        SmiResyncSubtitleServiceStrategy(),
        SrtResyncSubtitleServiceStrategy()
    )

    fun getStrategy(type: ResyncSubtitleType): ResyncSubtitleServiceStrategy {
        return strategies.find { it.type == type } ?: throw IllegalArgumentException("Unsupported type: $type")
    }
}

sealed class ResyncSubtitleServiceStrategy {
    abstract val type: ResyncSubtitleType
    abstract fun shiftSubtitle(file: Path, shiftMillis: Int)

    fun toOutputPath(file: Path): Path {
        return file.resolveSibling("${file.nameWithoutExtension}.shifted.${file.extension}")
    }
}

class SmiResyncSubtitleServiceStrategy : ResyncSubtitleServiceStrategy() {
    override val type = ResyncSubtitleType.SMI
    private val smiPattern = """(?i)(<SYNC\s*Start\s*=\s*)(\d+)(>.*?)(</SYNC>)?""".toRegex()

    override fun shiftSubtitle(file: Path, shiftMillis: Int) {
        val content = Files.readString(file)
        val shiftedContent = smiPattern.replace(content) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val startTime = matchResult.groupValues[2].toInt()
            val suffix = matchResult.groupValues[3]

            val newStartTime = (startTime + shiftMillis).coerceAtLeast(0)
            "$prefix$newStartTime$suffix"
        }

        Files.writeString(toOutputPath(file), shiftedContent)
    }
}

class SrtResyncSubtitleServiceStrategy : ResyncSubtitleServiceStrategy() {
    override val type = ResyncSubtitleType.SRT
    private val srtPattern = """
        (\d+)\s+(\d{2}:\d{2}:\d{2},\d{3})\s+-->\s+(\d{2}:\d{2}:\d{2},\d{3})\s+([\s\S]*?)\s*(?=\d+\s+\d{2}|\Z)
        """.trimIndent().toRegex()

    override fun shiftSubtitle(file: Path, shiftMillis: Int) {
        val content = Files.readString(file)
        val shiftedContent = srtPattern.replace(content) { matchResult ->
            val number = matchResult.groupValues[1]
            val startTime = matchResult.groupValues[2]
            val endTime = matchResult.groupValues[3]
            val subtitle = matchResult.groupValues[4]

            val newStartTime = shiftTime(startTime, shiftMillis)
            val newEndTime = shiftTime(endTime, shiftMillis)

            "$number\n$newStartTime --> $newEndTime\n$subtitle\n\n"
        }

        Files.writeString(toOutputPath(file), shiftedContent)
    }

    private fun shiftTime(time: String, shiftMs: Int): String {
        val hours = time.substring(0, 2).toLong()
        val minutes = time.substring(3, 5).toLong()
        val seconds = time.substring(6, 8).toLong()
        val milliseconds = time.substring(9, 12).toLong()

        val originalMillis = (((hours * 60 + minutes) * 60) + seconds) * 1_000 + milliseconds
        val shiftedMillis = (originalMillis + shiftMs).coerceAtLeast(0L)

        val newHours = shiftedMillis / 3_600_000
        val remainderAfterHours = shiftedMillis % 3_600_000
        val newMinutes = remainderAfterHours / 60_000
        val remainderAfterMinutes = remainderAfterHours % 60_000
        val newSeconds = remainderAfterMinutes / 1_000
        val newMillis = remainderAfterMinutes % 1_000

        return String.format("%02d:%02d:%02d,%03d", newHours, newMinutes, newSeconds, newMillis)
    }
}
