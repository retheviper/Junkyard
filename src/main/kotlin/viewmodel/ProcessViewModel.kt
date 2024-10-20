package viewmodel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.nio.file.Path
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.awt.datatransfer.DataFlavor
import java.io.File

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

    private val _job = MutableStateFlow<Job?>(null)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _total = MutableStateFlow(0F)
    private val _current = MutableStateFlow(0F)
    private val _progress = MutableStateFlow(0F)
    val progress = _progress.asStateFlow()

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
        _current.value = 0F
    }

    protected fun incrementProcessed() {
        _processed.value++
    }

    protected fun incrementFailed() {
        _failed.value++
    }

    protected fun setTotal(total: Int) {
        _total.value = total.toFloat()
    }

    protected fun incrementCurrent() {
        _current.value++
        _progress.value = if (_total.value == 0F) 0F else _current.value / _total.value
    }

    abstract fun onProcessClick()

    protected fun processWithCount(block: () -> Unit) {
        runCatching { block(); incrementCurrent() }
            .onSuccess { incrementProcessed() }
            .onFailure { incrementFailed(); it.printStackTrace() }
    }

    protected fun process(block: suspend (Path) -> Unit) {
        val basePath = path.value ?: return

        val job = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                startProcessing()
                runCatching { block(basePath) }
                    .also { stopProcessing() }
            }
        }

        _job.value = job
    }

    fun cancel() {
        _job.value?.cancel()
        _job.value = null
        stopProcessing()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun handleDrop(event: DragAndDropEvent, target: TargetPickerType): Boolean {
        val files = event.awtTransferable
            .let { transferable ->
                (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                    .takeIf { transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) }
            }?.filterIsInstance<File>()

        files?.forEach { file ->
            when (target) {
                TargetPickerType.DIRECTORY -> {
                    if (file.isDirectory) {
                        setPath(file.toPath())
                        return true
                    }
                }
                TargetPickerType.FILE -> {
                    if (file.isFile && targetExtensions.any { file.extension.equals(it, true) }) {
                        setPath(file.toPath())
                        return true
                    }
                }
            }
        }
        return false
    }
}