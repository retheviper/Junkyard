package application.usecase

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun ZipOutputStream.addZipEntry(origin: Path, zipEntry: Path) {
    putNextEntry(ZipEntry(zipEntry.toString().replace('\\', '/')))
    Files.copy(origin, this)
    closeEntry()
}

internal fun zipFiles(unarchivedFolder: Path, zipFilePath: Path) {
    val tempZipFile = Files.createTempFile(zipFilePath.toAbsolutePath().parent, "${zipFilePath.fileName}", ".tmp")
    runCatching {
        ZipOutputStream(Files.newOutputStream(tempZipFile)).use { zipOutputStream ->
            Files.walk(unarchivedFolder).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { zipOutputStream.addZipEntry(it, unarchivedFolder.relativize(it)) }
            }
        }

        moveReplacing(tempZipFile, zipFilePath)
    }.onFailure {
        Files.deleteIfExists(tempZipFile)
        throw it
    }
}

private fun moveReplacing(source: Path, target: Path) {
    runCatching {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }.getOrElse {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    }
}
