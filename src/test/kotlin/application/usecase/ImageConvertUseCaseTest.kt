package application.usecase

import application.model.ImageFromFormat
import application.processing.ProcessingContext
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import com.sksamuel.scrimage.webp.WebpWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ImageConvertUseCaseTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `converts an AVIF input to PNG`() = withTempDirectory { directory ->
        startKoin {
            modules(
                module {
                    single { PngWriter() }
                    single { JpegWriter.Default }
                    single { GifWriter.Default }
                    single { WebpWriter.DEFAULT }
                }
            )
        }

        val source = directory.resolve("image.avif")
        Files.write(source, AVIF_IMAGE_BYTES)

        val useCase = ImageConvertUseCase(
            webpImageReader = WebpImageReader(),
            imageIOReader = ImageIOReader(),
            gif2WebpWriter = Gif2WebpWriter.DEFAULT
        )
        var processed = 0
        var failed = 0

        runBlocking {
            useCase.execute(
                basePath = source,
                fromFormat = ImageFromFormat.ALL,
                toFormat = Format.PNG,
                includeArchiveFiles = false,
                context = ProcessingContext(
                    setTotalFn = {},
                    updateCurrentFileFn = {},
                    processWithCountFn = { it() },
                    incrementCurrentFn = {},
                    incrementProcessedFn = { processed++ },
                    incrementFailedFn = { failed++ },
                    printErrorFn = { throw it },
                    yieldFn = {}
                )
            )
        }

        val converted = directory.resolve("image.png")
        assertFalse(source.exists())
        assertTrue(converted.exists())
        assertEquals(1, processed)
        assertEquals(0, failed)
        ImageIO.read(converted.toFile()).let { image ->
            assertEquals(2, image.width)
            assertEquals(2, image.height)
        }
    }

    @Test
    fun `exposes AVIF as a selectable target extension`() {
        val useCase = ImageConvertUseCase(WebpImageReader(), ImageIOReader(), Gif2WebpWriter.DEFAULT)

        assertTrue("avif" in useCase.supportedTargetExtensions())
        assertTrue(ImageFromFormat.AVIF.matchesExtension("AVIF"))
        assertFalse(ImageFromFormat.AVIF.matchesExtension("webp"))
        assertFalse(ImageFromFormat.AVIF.matches(Format.WEBP))
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-avif-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private companion object {
        val AVIF_IMAGE_BYTES: ByteArray = Base64.getDecoder().decode(
            "AAAAIGZ0eXBhdmlmAAAAAE1pUHJhdmlmbWlhZm1pZjEAAAG3bWV0YQAAAAAAAAAhaGRscgAAAAAAAAAAcGljdAAAAAAA" +
                "AAAAAAAAAAAAAAAkZGluZgAAABxkcmVmAAAAAAAAAAEAAAAMdXJsIAAAAAEAAAAOcGl0bQAAAAAAAQAAADhpaW5mAAAA" +
                "AAACAAAAFWluZmUCAAAAAAEAAGF2MDEAAAAAFWluZmUCAAABAAIAAGF2MDEAAAAAGmlyZWYAAAAAAAAADmF1eGwAAgAB" +
                "AAEAAADaaXBycAAAALFpcGNvAAAAE2NvbHJuY2x4AAIAAgAGgAAAAAxjbGxpAMsAQAAAABRpc3BlAAAAAAAAAAIAAAAC" +
                "AAAACWlyb3QAAAAAEHBpeGkAAAAAAwgICAAAAA5waXhpAAAAAAEIAAAAN2F1eEMAAAAAdXJuOm1wZWc6aGV2YzoyMDE1" +
                "OmF1eGlkOjEAAAAADAAAAAhOAaUEAAH+QAAAAAxhdjFDgQAMAAAAAAxhdjFDgQAcAAAAACFpcG1hAAAAAAAAAAIAAQaB" +
                "AgMFiIQAAgUDBoeJhAAAACxpbG9jAAAAAEQAAAIAAQAAAAEAAAHnAAAALAACAAAAAQAAAhMAAAAXAAAAAW1kYXQAAAAA" +
                "AAAAUxIACgwAAAAABn/8CBAQNCAyGhABkgAIIIIgTrIxaEDGvZ49Az8fD/q++aR9EgAKCAAAAAAGf/wVMgkQAY4AIIg" +
                "BSCA="
        )
    }
}
