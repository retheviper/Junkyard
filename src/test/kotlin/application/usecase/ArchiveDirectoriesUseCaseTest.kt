package application.usecase

import application.processing.RecordingProcessingContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ArchiveDirectoriesUseCaseTest {
    @Test
    fun `stores images uncompressed and deflates other files`() = withTempDirectory { directory ->
        val subDirectory = directory.resolve("volume1").createDirectories()
        val imageBytes = ByteArray(4_096) { it.toByte() }
        Files.write(subDirectory.resolve("001.jpg"), imageBytes)
        Files.writeString(subDirectory.resolve("notes.txt"), "notes ".repeat(100))
        val context = RecordingProcessingContext()

        runBlocking {
            ArchiveDirectoriesUseCase().execute(directory, includeParentDirectory = true, context.toProcessingContext())
        }

        assertEquals(1, context.processed)
        assertEquals(setOf("volume1", "volume1.zip"), directory.listDirectoryEntries().map { it.name }.toSet())
        ZipFile(directory.resolve("volume1.zip").toFile()).use { zipFile ->
            val image = zipFile.getEntry("volume1/001.jpg")
            assertEquals(ZipEntry.STORED, image.method)
            assertContentEquals(imageBytes, zipFile.getInputStream(image).use { it.readBytes() })
            assertEquals(ZipEntry.DEFLATED, zipFile.getEntry("volume1/notes.txt").method)
        }
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-archive-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
