package viewmodel

import com.github.junrar.Junrar
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.nameWithoutExtension

class RarToZipViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    override fun onProcessClick() {
        process { basePath ->
            Files.walk(basePath, 1)
                .filter { Files.isDirectory(it) && it != basePath }
                .forEach { subDir -> convert(subDir) }
        }
    }

    private fun convert(subDir: Path) {
        Files.walk(subDir)
            .filter { it.toString().endsWith(".rar") }
            .forEach { rarFile ->
                val destinationFolder = subDir.resolve("unrar_${rarFile.fileName}")
                Files.createDirectory(destinationFolder)
                runCatching { Junrar.extract(rarFile.toFile(), destinationFolder.toFile()) }
                    .onFailure {
                        destinationFolder.toFile().deleteRecursively()
                        incrementFailed()
                        return@forEach
                    }

                zipFiles(subDir, rarFile, destinationFolder)

                destinationFolder.toFile().deleteRecursively()
                incrementProcessed()
            }
    }

    private fun zipFiles(subDir: Path, rarFile: Path, destinationFolder: Path) {
        val zipFilePath = subDir.resolve("${rarFile.nameWithoutExtension}.zip")
        ZipOutputStream(zipFilePath.toFile().outputStream()).use { zipOutputStream ->
            Files.walk(destinationFolder)
                .filter { Files.isRegularFile(it) }
                .forEach { file ->
                    val zipEntry = destinationFolder.relativize(file).toString()
                    zipOutputStream.putNextEntry(ZipEntry(zipEntry))
                    Files.copy(file, zipOutputStream)
                    zipOutputStream.closeEntry()
                }
        }
    }
}