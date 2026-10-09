package application.usecase

import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.extension

// Image data is already compressed: deflating it again costs far more time than the space it saves.
private val storedExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "avif")

internal fun ZipOutputStream.addZipEntry(origin: Path, zipEntry: Path) {
    val entry = ZipEntry(zipEntry.toString().replace('\\', '/'))
    if (origin.extension.lowercase() in storedExtensions) {
        val size = Files.size(origin)
        entry.method = ZipEntry.STORED
        entry.size = size
        entry.compressedSize = size
        entry.crc = crc32Of(origin)
    }
    putNextEntry(entry)
    Files.copy(origin, this)
    closeEntry()
}

// Zip streams read and write in small chunks, so they are always wrapped in buffered file streams.
internal fun newZipOutputStream(path: Path): ZipOutputStream =
    ZipOutputStream(Files.newOutputStream(path).buffered())

internal fun newZipInputStream(path: Path): ZipInputStream =
    ZipInputStream(Files.newInputStream(path).buffered())

internal fun zipFiles(unarchivedFolder: Path, zipFilePath: Path) {
    writeAtomically(zipFilePath) { tempZipFile ->
        newZipOutputStream(tempZipFile).use { zipOutputStream ->
            Files.walk(unarchivedFolder).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { zipOutputStream.addZipEntry(it, unarchivedFolder.relativize(it)) }
            }
        }
    }
}

private fun crc32Of(path: Path): Long =
    CheckedInputStream(Files.newInputStream(path), CRC32()).use { input ->
        input.copyTo(OutputStream.nullOutputStream())
        input.checksum.value
    }
