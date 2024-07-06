package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ProcessViewModel : ViewModel() {
    private val _path: MutableStateFlow<Path?> = MutableStateFlow(null)
    val path: StateFlow<Path?> = _path

    private val _processed = MutableStateFlow(0)
    val processed: StateFlow<Int> = _processed

    private val _failed = MutableStateFlow(0)
    val failed: StateFlow<Int> = _failed

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun setPath(path: Path) {
        _path.value = path
    }

    protected fun startProcessing() {
        _isProcessing.value = true
        _processed.value = 0
        _failed.value = 0
    }

    protected fun stopProcessing() {
        _isProcessing.value = false
    }

    protected fun incrementProcessed() {
        _processed.value++
    }

    protected fun incrementFailed() {
        _failed.value++
    }

    abstract fun onProcessClick()

    protected fun processWithCount(block: () -> Unit) {
        runCatching { block() }
            .onSuccess { incrementProcessed() }
            .onFailure { incrementFailed() }
    }

    protected fun process(block: (Path) -> Unit) {
        val basePath = path.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                startProcessing()
                try {
                    block(basePath)
                } finally {
                    stopProcessing()
                }
            }
        }

    }
}