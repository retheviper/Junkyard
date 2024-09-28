package service

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss,SSS")

    override fun shiftSubtitle(file: Path, shiftMillis: Int) {
        val content = Files.readString(file)
        val shiftedContent = srtPattern.replace(content) { matchResult ->
            val number = matchResult.groupValues[1]
            val startTime = matchResult.groupValues[2]
            val endTime = matchResult.groupValues[3]
            val subtitle = matchResult.groupValues[4]

            val newStartTime = shiftTime(startTime, shiftMillis, timeFormatter)
            val newEndTime = shiftTime(endTime, shiftMillis, timeFormatter)

            "$number\n$newStartTime --> $newEndTime\n$subtitle\n"
        }

        Files.writeString(toOutputPath(file), shiftedContent)
    }

    private fun shiftTime(time: String, shiftMs: Int, formatter: DateTimeFormatter): String {
        val localTime = LocalTime.parse(time, formatter)
        val shiftedTime = localTime.plusNanos(shiftMs * 1_000_000L)
        return shiftedTime.format(formatter)
    }
}

