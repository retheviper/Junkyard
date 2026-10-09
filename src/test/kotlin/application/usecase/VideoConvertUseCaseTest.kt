package application.usecase

import application.model.VideoCodec
import application.model.VideoFormat
import application.processing.RecordingProcessingContext
import infrastructure.binary.BinaryBundleService
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class VideoConvertUseCaseTest {
    @Test
    fun `keeps both videos when the converted file already exists`() = withTempDirectory { directory ->
        Files.writeString(directory.resolve("movie.avi"), "source")
        Files.writeString(directory.resolve("movie.mp4"), "existing")
        val context = RecordingProcessingContext()

        runBlocking {
            VideoConvertUseCase(BinaryBundleService()).execute(
                basePath = directory,
                targetFormat = VideoFormat.ALL,
                videoCodec = VideoCodec.H264,
                useHardwareEncoder = false,
                context = context.toProcessingContext()
            )
        }

        assertEquals(1, context.failed)
        assertIs<FileAlreadyExistsException>(context.errors.single())
        assertEquals("source", directory.resolve("movie.avi").readText())
        assertEquals("existing", directory.resolve("movie.mp4").readText())
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-video-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
