package viewmodel

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ArchiveViewModel : ProcessViewModel() {
    private val _isParentDirectoryIncluded = MutableStateFlow(false)
    val isParentDirectoryIncluded: StateFlow<Boolean> = _isParentDirectoryIncluded

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
            .forEach { file ->
                val zipEntry = basePath.relativize(file).toString()
                zipOutputStream.putNextEntry(ZipEntry(zipEntry))
                Files.copy(file, zipOutputStream)
                zipOutputStream.closeEntry()
            }
    }

    private fun zipFilesOnly(subDir: Path?, zipOutputStream: ZipOutputStream) {
        Files.walk(subDir, 1)
            .filter { Files.isRegularFile(it) }
            .forEach { file ->
                val zipEntry = file.fileName.toString()
                zipOutputStream.putNextEntry(ZipEntry(zipEntry))
                Files.copy(file, zipOutputStream)
                zipOutputStream.closeEntry()
            }
    }
}