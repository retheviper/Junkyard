package application.usecase

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun ZipOutputStream.addZipEntry(origin: Path, zipEntry: Path) {
    putNextEntry(ZipEntry(zipEntry.toString()))
    Files.copy(origin, this)
    closeEntry()
}

internal fun zipFiles(unarchivedFolder: Path, zipFilePath: Path) {
    ZipOutputStream(Files.newOutputStream(zipFilePath)).use { zipOutputStream ->
        Files.walk(unarchivedFolder).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .forEach { zipOutputStream.addZipEntry(it, unarchivedFolder.relativize(it)) }
        }
    }
}
