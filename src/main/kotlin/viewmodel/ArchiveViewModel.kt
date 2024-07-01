package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArchiveViewModel : ViewModel() {
    private val _path: MutableStateFlow<Path?> = MutableStateFlow(null)
    val path: StateFlow<Path?> = _path

    private val _isParentDirectoryIncluded = MutableStateFlow(false)
    val isParentDirectoryIncluded: StateFlow<Boolean> = _isParentDirectoryIncluded

    private val _isArchiving = MutableStateFlow(false)
    val isArchiving: StateFlow<Boolean> = _isArchiving

    fun setPath(path: Path) {
        _path.value = path
    }

    fun toggleParentDirectory() {
        _isParentDirectoryIncluded.value = !_isParentDirectoryIncluded.value
    }

    fun onArchiveClick() {
        val basePath = path.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _isArchiving.value = true
                try {
                    Files.walk(basePath, 1)
                        .filter { Files.isDirectory(it) && it != basePath }
                        .forEach { subDir ->
                            val zipFilePath = basePath.resolve("${subDir.fileName}.zip")
                            ZipOutputStream(zipFilePath.toFile().outputStream()).use { zipOutputStream ->
                                if (isParentDirectoryIncluded.value) {
                                    // Include the parent directory
                                    Files.walk(subDir)
                                        .filter { Files.isRegularFile(it) }
                                        .forEach { file ->
                                            val zipEntry = basePath.relativize(file).toString()
                                            zipOutputStream.putNextEntry(ZipEntry(zipEntry))
                                            Files.copy(file, zipOutputStream)
                                            zipOutputStream.closeEntry()
                                        }
                                } else {
                                    // Include only files in the subdirectory
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
                        }
                } finally {
                    _isArchiving.value = false
                }
            }
        }

    }
}