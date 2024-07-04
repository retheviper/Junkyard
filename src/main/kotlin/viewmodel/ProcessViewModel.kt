package viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ProcessViewModel : ViewModel() {
    private val _processed = MutableStateFlow(0)
    val processed: StateFlow<Int> = _processed

    private val _failed = MutableStateFlow(0)
    val failed: StateFlow<Int> = _failed

    protected fun incrementProcessed() {
        _processed.value++
    }

    protected fun incrementFailed() {
        _failed.value++
    }
}