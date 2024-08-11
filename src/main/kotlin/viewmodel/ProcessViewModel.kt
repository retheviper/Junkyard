package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

enum class TargetPickerType {
    DIRECTORY,
    FILE
}

abstract class ProcessViewModel : ViewModel(), KoinComponent {
    abstract val targetPickerType: TargetPickerType
    open val targetExtensions: List<String> = emptyList()

    private val _path: MutableStateFlow<Path?> = MutableStateFlow(null)
    val path = _path.asStateFlow()

    private val _processed = MutableStateFlow(0)
    val processed = _processed.asStateFlow()

    private val _failed = MutableStateFlow(0)
    val failed = _failed.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun setPath(path: Path) {
        _path.value = path
    }

    private fun startProcessing() {
        _isProcessing.value = true
        _processed.value = 0
        _failed.value = 0
    }

    private fun stopProcessing() {
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