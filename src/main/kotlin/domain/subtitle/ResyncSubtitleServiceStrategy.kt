package domain.subtitle

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
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

    /** Matches the timing text to shift. Everything outside the matches is written back unchanged. */
    protected abstract val timingPattern: Regex

    protected abstract fun shiftTiming(match: MatchResult, shiftMillis: Int): String

    fun shiftSubtitle(file: Path, shiftMillis: Int) {
        val bytes = Files.readAllBytes(file)
        val charset = detectCharset(bytes)
        val content = String(bytes, charset)
        require(timingPattern.containsMatchIn(content)) { "No $type timing found in ${file.fileName}" }

        val shiftedContent = timingPattern.replace(content) { shiftTiming(it, shiftMillis) }
        Files.write(toOutputPath(file), shiftedContent.toByteArray(charset))
    }

    fun toOutputPath(file: Path): Path {
        return file.resolveSibling("${file.nameWithoutExtension}.shifted.${file.extension}")
    }

    /**
     * Subtitles come in many encodings (UTF-8, CP949, Shift_JIS, ...), and only ASCII timing text is rewritten.
     * ISO-8859-1 maps every byte to one char, so text in any ASCII-compatible encoding round-trips byte for byte.
     * UTF-16 is not ASCII-compatible, so it is decoded as such when its byte order mark is present.
     */
    private fun detectCharset(bytes: ByteArray): Charset = when {
        bytes.startsWith(0xFF, 0xFE) -> Charsets.UTF_16LE
        bytes.startsWith(0xFE, 0xFF) -> Charsets.UTF_16BE
        else -> Charsets.ISO_8859_1
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it].toByte() }
}

class SmiResyncSubtitleServiceStrategy : ResyncSubtitleServiceStrategy() {
    override val type = ResyncSubtitleType.SMI
    override val timingPattern = """(?i)(<SYNC\s*Start\s*=\s*["']?)(\d+)""".toRegex()

    override fun shiftTiming(match: MatchResult, shiftMillis: Int): String {
        val (prefix, startTime) = match.destructured
        val newStartTime = (startTime.toLong() + shiftMillis).coerceAtLeast(0)
        return "$prefix$newStartTime"
    }
}

class SrtResyncSubtitleServiceStrategy : ResyncSubtitleServiceStrategy() {
    override val type = ResyncSubtitleType.SRT
    override val timingPattern = """(\d{2,}:\d{2}:\d{2},\d{3})(\s*-->\s*)(\d{2,}:\d{2}:\d{2},\d{3})""".toRegex()

    override fun shiftTiming(match: MatchResult, shiftMillis: Int): String {
        val (startTime, arrow, endTime) = match.destructured
        return "${shiftTime(startTime, shiftMillis)}$arrow${shiftTime(endTime, shiftMillis)}"
    }

    private fun shiftTime(time: String, shiftMs: Int): String {
        val (hours, minutes, seconds, milliseconds) = time.split(':', ',').map { it.toLong() }

        val originalMillis = (((hours * 60 + minutes) * 60) + seconds) * 1_000 + milliseconds
        val shiftedMillis = (originalMillis + shiftMs).coerceAtLeast(0L)

        val newHours = shiftedMillis / 3_600_000
        val remainderAfterHours = shiftedMillis % 3_600_000
        val newMinutes = remainderAfterHours / 60_000
        val remainderAfterMinutes = remainderAfterHours % 60_000
        val newSeconds = remainderAfterMinutes / 1_000
        val newMillis = remainderAfterMinutes % 1_000

        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", newHours, newMinutes, newSeconds, newMillis)
    }
}
