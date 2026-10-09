package application.usecase

import application.model.CreateThumbnailOption
import application.model.ImageOutputFormat
import application.processing.RecordingProcessingContext
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import com.sksamuel.scrimage.webp.WebpWriter
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class CreateThumbnailUseCaseTest {
    private val useCase = CreateThumbnailUseCase(WebpImageReader(), ImageIOReader(), StreamingGifWriter())

    @BeforeTest
    fun setUp() {
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
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `creates a thumbnail in the original format`() = withTempDirectory { directory ->
        Files.write(directory.resolve("photo.png"), ImmutableImage.create(40, 20).bytes(PngWriter.MaxCompression))
        val context = RecordingProcessingContext()

        createThumbnails(directory, ImageOutputFormat.ORIGINAL, context)

        assertEquals(1, context.processed)
        assertEquals(0, context.failed)
        ImageIO.read(directory.resolve("photo_thumbnail.png").toFile()).let { thumbnail ->
            assertEquals(10, thumbnail.width)
            assertEquals(10, thumbnail.height)
        }
    }

    @Test
    fun `skips directories and non-image files while completing the progress`() = withTempDirectory { directory ->
        directory.resolve("album.png").createDirectory()
        Files.writeString(directory.resolve("notes.png"), "not an image")
        Files.write(directory.resolve("photo.jpg"), ImmutableImage.create(40, 20).bytes(JpegWriter.Default))
        val context = RecordingProcessingContext()

        createThumbnails(directory, ImageOutputFormat.JPEG, context)

        assertEquals(2, context.total)
        assertEquals(context.total, context.current)
        assertEquals(1, context.processed)
        assertEquals(0, context.failed)
        assertTrue(directory.resolve("photo_thumbnail.jpg").exists())
    }

    private fun createThumbnails(directory: Path, outputFormat: ImageOutputFormat, context: RecordingProcessingContext) {
        runBlocking {
            useCase.execute(
                basePath = directory,
                targetFormats = setOf(Format.JPEG, Format.PNG),
                outputFormat = outputFormat,
                option = CreateThumbnailOption.FIXED_SIZE,
                width = 10,
                height = 10,
                ratio = 50.0,
                context = context.toProcessingContext()
            )
        }
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-thumbnail-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
