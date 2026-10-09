package presentation.viewmodel

import application.usecase.ImageConvertUseCase
import com.sksamuel.scrimage.ImmutableImage
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
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ImageConvertViewModelTest {
    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { PngWriter() }
                    single { JpegWriter.Default }
                    single { GifWriter.Default }
                    single { WebpWriter.DEFAULT }
                    single { ImageConvertUseCase(WebpImageReader(), ImageIOReader(), Gif2WebpWriter.DEFAULT) }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `warns about images too large for WebP and converts nothing when declined`() = withImages { directory, small, tall ->
        val viewModel = ImageConvertViewModel().apply { setPath(directory) }

        viewModel.onProcessClick()
        val warning = awaitWarning(viewModel)
        viewModel.respondToWarning(proceed = false)
        awaitIdle(viewModel)

        assertEquals(Format.WEBP, warning.format)
        assertEquals(16_383, warning.maxDimension)
        assertEquals(listOf("tall.png"), warning.images.map { it.path })
        assertTrue(small.exists())
        assertTrue(tall.exists())
        assertEquals(0, viewModel.processed.value)
        assertEquals("Canceled.", viewModel.logs.value.last())
    }

    @Test
    fun `converts the remaining images when the warning is accepted`() = withImages { directory, small, tall ->
        val viewModel = ImageConvertViewModel().apply { setPath(directory) }

        viewModel.onProcessClick()
        awaitWarning(viewModel)
        viewModel.respondToWarning(proceed = true)
        awaitIdle(viewModel)

        assertFalse(small.exists())
        assertTrue(directory.resolve("small.webp").exists())
        assertTrue(tall.exists())
        assertEquals(1, viewModel.processed.value)
        assertEquals(1, viewModel.failed.value)
    }

    private fun awaitWarning(viewModel: ProcessViewModel): ProcessWarning.ImageSizeLimitExceeded = runBlocking {
        assertIs<ProcessWarning.ImageSizeLimitExceeded>(
            withTimeout(TIMEOUT_MILLIS) { viewModel.warning.filterNotNull().first() }
        )
    }

    private fun awaitIdle(viewModel: ProcessViewModel) = runBlocking {
        withTimeout(TIMEOUT_MILLIS) { viewModel.isProcessing.first { !it } }
    }

    private fun withImages(block: (directory: Path, small: Path, tall: Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-viewmodel-test")
        try {
            val small = directory.resolve("small.png")
            Files.write(small, ImmutableImage.create(2, 2).bytes(PngWriter.MaxCompression))
            val tall = directory.resolve("tall.png")
            Files.write(tall, ImmutableImage.create(1, 16_384).bytes(PngWriter.MaxCompression))
            block(directory, small, tall)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 60_000L
    }
}
