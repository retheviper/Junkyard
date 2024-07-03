package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeExtensionViewModel : ViewModel() {
    private val _path: MutableStateFlow<Path?> = MutableStateFlow(null)
    val path: StateFlow<Path?> = _path

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting

    fun setPath(path: Path) {
        _path.value = path
    }

    fun onConvertClick(fromExtension: String, toExtension: String, ignoreCase: Boolean) {
        val basePath = path.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _isConverting.value = true
                try {
                    Files.walk(basePath)
                        .filter { it.extension.equals(fromExtension, ignoreCase) }
                        .forEach {
                            val newFileName = it.nameWithoutExtension + toExtension
                            Files.move(it, it.resolveSibling(newFileName))
                        }
                } finally {
                    _isConverting.value = false
                }
            }
        }

    }
}