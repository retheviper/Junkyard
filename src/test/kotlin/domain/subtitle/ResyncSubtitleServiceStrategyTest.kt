package domain.subtitle

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ResyncSubtitleServiceStrategyTest {
    private val strategyFactory = ResyncSubtitleServiceStrategyFactory()

    @Test
    fun `shifts SRT timings while keeping the text and line endings`() = withTempDirectory { directory ->
        val source = directory.resolve("movie.srt")
        Files.writeString(
            source,
            "1\r\n00:00:01,000 --> 00:00:02,500\r\nCall 555 12 now\r\n\r\n" +
                "2\r\n00:00:03,000 --> 00:00:04,000\r\nBye\r\n"
        )

        strategyFactory.getStrategy(ResyncSubtitleType.SRT).shiftSubtitle(source, -1_500)

        assertEquals(
            "1\r\n00:00:00,000 --> 00:00:01,000\r\nCall 555 12 now\r\n\r\n" +
                "2\r\n00:00:01,500 --> 00:00:02,500\r\nBye\r\n",
            Files.readString(directory.resolve("movie.shifted.srt"))
        )
    }

    @Test
    fun `keeps SMI text in a legacy encoding byte for byte`() = withTempDirectory { directory ->
        val cp949 = Charset.forName("MS949")
        val source = directory.resolve("movie.smi")
        val subtitle = "<SAMI><BODY>\r\n<SYNC Start=1000><P Class=KRCC>안녕하세요\r\n" +
            "<SYNC Start=2500></SYNC>\r\n</BODY></SAMI>\r\n"
        Files.write(source, subtitle.toByteArray(cp949))

        strategyFactory.getStrategy(ResyncSubtitleType.SMI).shiftSubtitle(source, 500)

        val expected = subtitle.replace("Start=1000", "Start=1500").replace("Start=2500", "Start=3000")
        assertContentEquals(expected.toByteArray(cp949), Files.readAllBytes(directory.resolve("movie.shifted.smi")))
    }

    @Test
    fun `shifts UTF-16 subtitles with a byte order mark`() = withTempDirectory { directory ->
        val source = directory.resolve("movie.srt")
        val subtitle = "﻿1\r\n00:00:01,000 --> 00:00:02,000\r\n자막\r\n"
        Files.write(source, subtitle.toByteArray(Charsets.UTF_16LE))

        strategyFactory.getStrategy(ResyncSubtitleType.SRT).shiftSubtitle(source, 1_000)

        val expected = subtitle.replace("00:00:01,000 --> 00:00:02,000", "00:00:02,000 --> 00:00:03,000")
        assertContentEquals(
            expected.toByteArray(Charsets.UTF_16LE),
            Files.readAllBytes(directory.resolve("movie.shifted.srt"))
        )
    }

    @Test
    fun `fails when the file contains no subtitle timing`() = withTempDirectory { directory ->
        val source = directory.resolve("movie.srt")
        Files.writeString(source, "not a subtitle")

        assertFailsWith<IllegalArgumentException> {
            strategyFactory.getStrategy(ResyncSubtitleType.SRT).shiftSubtitle(source, 1_000)
        }
        assertFalse(directory.resolve("movie.shifted.srt").exists())
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-subtitle-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
