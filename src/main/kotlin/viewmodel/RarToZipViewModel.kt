package viewmodel

import com.github.junrar.Junrar
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipOutputStream
import kotlin.io.path.nameWithoutExtension

class RarToZipViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY
    private val rarExtension = "rar"

    override fun onProcessClick() {
        process { basePath ->
            Files.walk(basePath, 1)
                .filter { Files.isDirectory(it) && it != basePath }
                .forEach { subDir -> convert(subDir) }
        }
    }

    private fun convert(subDir: Path) {
        Files.walk(subDir)
            .filter { it.toString().endsWith(rarExtension, ignoreCase = true) }
            .forEach { rarFile ->
                val unarchivedFolder = subDir.resolve("unrar_${rarFile.fileName}")
                Files.createDirectory(unarchivedFolder)
                runCatching { Junrar.extract(rarFile.toFile(), unarchivedFolder.toFile()) }
                    .onFailure {
                        unarchivedFolder.toFile().deleteRecursively()
                        incrementFailed()
                        return@forEach
                    }

                zipFiles(subDir, rarFile, unarchivedFolder)

                unarchivedFolder.toFile().deleteRecursively()
                incrementProcessed()
            }
    }

    private fun zipFiles(subDir: Path, rarFile: Path, unarchivedFolder: Path) {
        val zipFilePath = subDir.resolve("${rarFile.nameWithoutExtension}.zip")
        zipFiles(unarchivedFolder, zipFilePath)
    }
}

internal fun zipFiles(unarchivedFolder: Path, zipFilePath: Path) {
    ZipOutputStream(Files.newOutputStream(zipFilePath)).use { zipOutputStream ->
        Files.walk(unarchivedFolder)
            .filter { Files.isRegularFile(it) }
            .forEach { zipOutputStream.addZipEntry(unarchivedFolder.relativize(it)) }
    }
}
