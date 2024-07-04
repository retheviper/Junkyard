package viewmodel

import androidx.lifecycle.ViewModel
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    protected fun toggleProcessing() {
        _isProcessing.value = !_isProcessing.value
    }

    protected fun incrementProcessed() {
        _processed.value++
    }

    protected fun incrementFailed() {
        _failed.value++
    }

    abstract fun onProcessClick()
}