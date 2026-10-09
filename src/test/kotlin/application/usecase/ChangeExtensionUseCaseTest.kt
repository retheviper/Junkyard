package application.usecase

import application.processing.RecordingProcessingContext
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class ChangeExtensionUseCaseTest {
    private val useCase = ChangeExtensionUseCase()

    @Test
    fun `changes only the case of an extension`() = withTempDirectory { directory ->
        Files.writeString(directory.resolve("photo.JPG"), "content")
        val context = RecordingProcessingContext()

        runBlocking { useCase.execute(directory, "JPG", "jpg", ignoreCase = false, context.toProcessingContext()) }

        assertEquals(1, context.processed)
        val renamed = directory.listDirectoryEntries().single()
        assertEquals("photo.jpg", renamed.name)
        assertEquals("content", renamed.readText())
    }

    @Test
    fun `fails instead of overwriting an existing file`() = withTempDirectory { directory ->
        Files.writeString(directory.resolve("photo.jpeg"), "source")
        Files.writeString(directory.resolve("photo.jpg"), "existing")
        val context = RecordingProcessingContext()

        runBlocking { useCase.execute(directory, "jpeg", "jpg", ignoreCase = false, context.toProcessingContext()) }

        assertEquals(1, context.failed)
        assertIs<FileAlreadyExistsException>(context.errors.single())
        assertEquals("source", directory.resolve("photo.jpeg").readText())
        assertEquals("existing", directory.resolve("photo.jpg").readText())
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-extension-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
