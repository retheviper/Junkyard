package viewmodel

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield

class ArchiveViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    private val _isParentDirectoryIncluded = MutableStateFlow(false)
    val isParentDirectoryIncluded = _isParentDirectoryIncluded.asStateFlow()

    fun toggleParentDirectory() {
        _isParentDirectoryIncluded.value = !_isParentDirectoryIncluded.value
    }

    override fun onProcessClick() {
        process { basePath ->
            val targets = Files.walk(basePath, 1)
                .filter { Files.isDirectory(it) && it != basePath }
                .toList()

            setTotal(targets.size)

            targets.forEach { subDir ->
                yield()
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
            .forEach { zipOutputStream.addZipEntry(it, basePath.relativize(it)) }
    }

    private fun zipFilesOnly(subDir: Path?, zipOutputStream: ZipOutputStream) {
        Files.walk(subDir, 1)
            .filter { Files.isRegularFile(it) }
            .forEach { zipOutputStream.addZipEntry(it, it.fileName) }
    }
}

internal fun ZipOutputStream.addZipEntry(origin: Path, zipEntry: Path) {
    putNextEntry(ZipEntry(zipEntry.toString()))
    Files.copy(origin, this)
    closeEntry()
}