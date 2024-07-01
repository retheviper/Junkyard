package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.junrar.Junrar
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RarToZipViewModel : ViewModel() {
    private val _path: MutableStateFlow<Path?> = MutableStateFlow(null)
    val path: StateFlow<Path?> = _path

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting

    private val _convertedFiles = MutableStateFlow(0)
    val convertedFiles: StateFlow<Int> = _convertedFiles

    private val _failedFiles = MutableStateFlow(0)
    val failedFiles: StateFlow<Int> = _failedFiles

    fun setPath(path: Path) {
        _path.value = path
    }

    fun onConvertClick() {
        val basePath = path.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _isConverting.value = true
                try {
                    Files.walk(basePath, 1)
                        .filter { Files.isDirectory(it) && it != basePath }
                        .forEach { subDir ->
                            convert(subDir)
                        }
                } finally {
                    _isConverting.value = false
                }
            }
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
                        _failedFiles.value += 1
                        return@forEach
                    }

                zipFiles(subDir, rarFile, destinationFolder)

                destinationFolder.toFile().deleteRecursively()
                _convertedFiles.value += 1
            }
    }

    private fun zipFiles(subDir: Path, rarFile: Path, destinationFolder: Path) {
        val zipFilePath = subDir.resolve("${rarFile.fileName.nameWithoutExtension}.zip")
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