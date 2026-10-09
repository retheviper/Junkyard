package application.usecase

import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileSupportTest {
    @Test
    fun `gives the written file the same permissions as other new files`() = withTempDirectory { directory ->
        if ("posix" !in FileSystems.getDefault().supportedFileAttributeViews()) return@withTempDirectory
        val newFile = Files.createFile(directory.resolve("new.txt"))
        val target = directory.resolve("written.txt")

        writeAtomically(target) { it.writeText("content") }

        assertEquals("content", target.readText())
        assertEquals(Files.getPosixFilePermissions(newFile), Files.getPosixFilePermissions(target))
    }

    @Test
    fun `keeps the existing file and leaves nothing behind when the write fails`() = withTempDirectory { directory ->
        val target = directory.resolve("target.txt")
        target.writeText("original")

        assertFailsWith<IOException> {
            writeAtomically(target) { tempFile ->
                tempFile.writeText("partial")
                throw IOException("write failed")
            }
        }

        assertEquals("original", target.readText())
        assertEquals(listOf("target.txt"), directory.listDirectoryEntries().map { it.name })
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-file-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
