package viewmodel

import com.github.junrar.Junrar
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipOutputStream
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.yield

class RarToZipViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY
    private val rarExtension = "rar"

    override fun onProcessClick() {
        process { basePath ->
            val targets = Files.walk(basePath).use { stream ->
                stream.filter { Files.isRegularFile(it) && it != basePath }
                    .filter { it.extension.equals(rarExtension, ignoreCase = true) }
                    .toList()
            }

            setTotal(targets.size)

            targets.forEach {
                yield()
                updateCurrentFile(it)
                convert(it)
            }
        }
    }

    private fun convert(rarFile: Path) {
        val unarchivedFolder = rarFile.resolveSibling("unrar_${rarFile.nameWithoutExtension}")
        Files.createDirectory(unarchivedFolder)
        incrementCurrent()
        runCatching { Junrar.extract(rarFile.toFile(), unarchivedFolder.toFile()) }
            .onFailure {
                unarchivedFolder.toFile().deleteRecursively()
                incrementFailed()
                return
            }

        val zipFilePath = rarFile.resolveSibling("${rarFile.nameWithoutExtension}.zip")
        zipFiles(unarchivedFolder, zipFilePath)

        unarchivedFolder.toFile().deleteRecursively()
        incrementProcessed()
    }
}

internal fun zipFiles(unarchivedFolder: Path, zipFilePath: Path) {
    ZipOutputStream(Files.newOutputStream(zipFilePath)).use { zipOutputStream ->
        Files.walk(unarchivedFolder).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .forEach { zipOutputStream.addZipEntry(it, unarchivedFolder.relativize(it)) }
        }
    }
}
