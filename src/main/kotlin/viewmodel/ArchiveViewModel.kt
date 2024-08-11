package viewmodel

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ArchiveViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    private val _isParentDirectoryIncluded = MutableStateFlow(false)
    val isParentDirectoryIncluded = _isParentDirectoryIncluded.asStateFlow()

    fun toggleParentDirectory() {
        _isParentDirectoryIncluded.value = !_isParentDirectoryIncluded.value
    }

    override fun onProcessClick() {
        process { basePath ->
            Files.walk(basePath, 1)
                .filter { Files.isDirectory(it) && it != basePath }
                .forEach { subDir ->
                    val zipFilePath = basePath.resolve("${subDir.fileName}.zip")
                    processWithCount {
                        ZipOutputStream(zipFilePath.toFile().outputStream()).use { zipOutputStream ->
                            if (isParentDirectoryIncluded.value) {
                                // Include the parent directory
                                zipDirectory(subDir, basePath, zipOutputStream)
                            } else {
                                // Include only files in the subdirectory
                                zipFilesOnly(subDir, zipOutputStream)
                            }
                        }
                    }
                }
        }
    }

    private fun zipDirectory(subDir: Path?, basePath: Path, zipOutputStream: ZipOutputStream) {
        Files.walk(subDir)
            .filter { Files.isRegularFile(it) }
            .forEach { zipOutputStream.addZipEntry(basePath.relativize(it)) }
    }

    private fun zipFilesOnly(subDir: Path?, zipOutputStream: ZipOutputStream) {
        Files.walk(subDir, 1)
            .filter { Files.isRegularFile(it) }
            .forEach { zipOutputStream.addZipEntry(it.fileName) }
    }
}

internal fun ZipOutputStream.addZipEntry(path: Path) {
    putNextEntry(ZipEntry(path.toString()))
    Files.copy(path, this)
    closeEntry()
}