package presentation.viewmodel

import application.model.OversizedImage
import application.processing.ProcessingContext
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sksamuel.scrimage.format.Format
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.awt.datatransfer.DataFlavor
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent

enum class TargetPickerType {
    DIRECTORY,
    FILE,
    BOTH
}

/** A warning the user has to accept before processing continues. */
sealed interface ProcessWarning {
    data class ImageSizeLimitExceeded(
        val format: Format,
        val maxDimension: Int,
        val images: List<OversizedImage>
    ) : ProcessWarning
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

    private val _currentFile = MutableStateFlow("")
    val currentFile = _currentFile.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _warning = MutableStateFlow<ProcessWarning?>(null)
    val warning = _warning.asStateFlow()

    @Volatile
    private var warningResponse: CompletableDeferred<Boolean>? = null

    fun setPath(path: Path) {
        _path.value = path
    }

    private fun startProcessing() {
        _isProcessing.value = true
        _processed.value = 0
        _failed.value = 0
        _logs.value = emptyList()
    }

    private fun stopProcessing() {
        _isProcessing.value = false
        _current.value = 0F
        _total.value = 0F
        _progress.value = 0F
        _currentFile.value = ""
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

    protected fun createProcessingContext(): ProcessingContext {
        return ProcessingContext(
            setTotalFn = ::setTotal,
            updateCurrentFileFn = ::updateCurrentFile,
            processWithCountFn = ::processWithCount,
            incrementCurrentFn = ::incrementCurrent,
            incrementProcessedFn = ::incrementProcessed,
            incrementFailedFn = ::incrementFailed,
            printErrorFn = ::recordError,
            yieldFn = { yield() }
        )
    }

    abstract fun onProcessClick()

    protected fun updateCurrentFile(file: Path) {
        val fileName = file.fileName.toString()
        _currentFile.value = fileName.takeIf { it.length <= 15 } ?: "${fileName.take(10)}...${fileName.takeLast(5)}"
    }

    protected fun recordLog(message: String) {
        _logs.value += message
    }

    private fun recordError(error: Throwable) {
        val stackTrace = StringWriter().also { writer ->
            error.printStackTrace(PrintWriter(writer))
        }.toString().trim()
        recordLog(stackTrace)
        error.printStackTrace()
    }

    protected fun processWithCount(block: () -> Unit) {
        runCatching { block() }
            .onSuccess { incrementProcessed() }
            .onFailure {
                if (it is CancellationException) {
                    throw it
                }
                incrementFailed()
                recordError(it)
            }
            .also { incrementCurrent() }
    }

    protected fun process(block: suspend (Path) -> Unit) {
        val basePath = path.value ?: return
        // A cancelled job keeps running until its current file is done, so a new run waits for it to complete.
        if (_job.value?.isCompleted == false) {
            return
        }

        startProcessing()
        _job.value = viewModelScope.launch(Dispatchers.IO) {
            recordLog("Started: $basePath")
            try {
                block(basePath)
                recordLog("Completed. Success: ${processed.value}, Failed: ${failed.value}")
            } catch (error: Throwable) {
                if (isActive) {
                    recordError(error)
                } else {
                    recordLog("Canceled.")
                }
                if (error is CancellationException) {
                    throw error
                }
            } finally {
                stopProcessing()
            }
        }
    }

    /** Shows [warning] and suspends until the user responds. Declining cancels the processing. */
    protected suspend fun confirmWarning(warning: ProcessWarning) {
        val response = CompletableDeferred<Boolean>()
        warningResponse = response
        _warning.value = warning
        val proceed = try {
            response.await()
        } finally {
            _warning.value = null
            warningResponse = null
        }

        if (!proceed) {
            currentCoroutineContext().cancel()
            currentCoroutineContext().ensureActive()
        }
    }

    fun respondToWarning(proceed: Boolean) {
        warningResponse?.complete(proceed)
    }

    fun cancel() {
        _job.value?.cancel()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun handleDrop(event: DragAndDropEvent, target: TargetPickerType): Boolean {
        val files = event.awtTransferable
            .let { transferable ->
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                } else {
                    null
                }
            }?.filterIsInstance<File>()

        val isTargetFile: (File) -> Boolean = { file ->
            file.isFile && (targetExtensions.isEmpty() || targetExtensions.any { file.extension.equals(it, true) })
        }
        val isTarget = when (target) {
            TargetPickerType.DIRECTORY -> File::isDirectory
            TargetPickerType.FILE -> isTargetFile
            TargetPickerType.BOTH -> { file -> file.isDirectory || isTargetFile(file) }
        }

        return files?.firstOrNull { isTarget(it) }
            ?.let { setPath(it.toPath()); true } == true
    }
}
