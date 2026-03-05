package application.usecase

import application.processing.ProcessingContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipOutputStream

class ArchiveDirectoriesUseCase {
    suspend fun execute(basePath: Path, includeParentDirectory: Boolean, context: ProcessingContext) {
        val targets = Files.walk(basePath, 1).use { stream ->
            stream
                .filter { Files.isDirectory(it) && it != basePath }
                .toList()
        }

        context.setTotal(targets.size)

        targets.forEach { subDir ->
            context.checkpoint()
            context.updateCurrentFile(subDir)
            val zipFilePath = basePath.resolve("${subDir.fileName}.zip")
            context.processWithCount {
                ZipOutputStream(zipFilePath.toFile().outputStream()).use { zipOutputStream ->
                    if (includeParentDirectory) {
                        zipDirectory(subDir, basePath, zipOutputStream)
                    } else {
                        zipFilesOnly(subDir, zipOutputStream)
                    }
                }
            }
        }
    }

    private fun zipDirectory(subDir: Path?, basePath: Path, zipOutputStream: ZipOutputStream) {
        subDir?.let {
            Files.walk(it).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { zipOutputStream.addZipEntry(it, basePath.relativize(it)) }
            }
        }
    }

    private fun zipFilesOnly(subDir: Path?, zipOutputStream: ZipOutputStream) {
        subDir?.let {
            Files.walk(it, 1).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { zipOutputStream.addZipEntry(it, it.fileName) }
            }
        }
    }
}
