package viewmodel

import androidx.lifecycle.viewModelScope
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeExtensionViewModel : ProcessViewModel() {
    private val _ignoreCase = MutableStateFlow(false)
    val ignoreCase: MutableStateFlow<Boolean> = _ignoreCase

    private val _fromExtension = MutableStateFlow("jpeg")
    val fromExtension: MutableStateFlow<String> = _fromExtension

    private val _toExtension = MutableStateFlow("jpg")
    val toExtension: MutableStateFlow<String> = _toExtension

    fun toggleIgnoreCase() {
        _ignoreCase.value = !_ignoreCase.value
    }

    fun setFromExtension(extension: String) {
        _fromExtension.value = extension
    }

    fun setToExtension(extension: String) {
        _toExtension.value = extension
    }

    override fun onProcessClick() {
        val basePath = path.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                toggleProcessing()
                try {
                    Files.walk(basePath)
                        .filter { it.extension.equals(fromExtension.value, ignoreCase.value) }
                        .forEach {
                            val newFileName = it.nameWithoutExtension + toExtension.value
                            runCatching {
                                Files.move(it, it.resolveSibling(newFileName))
                            }.onSuccess {
                                incrementProcessed()
                            }.onFailure {
                                incrementFailed()
                            }
                        }
                } finally {
                    toggleProcessing()
                }
            }
        }

    }
}