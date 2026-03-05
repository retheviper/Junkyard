package application.processing

import java.nio.file.Path

class ProcessingContext(
    private val setTotalFn: (Int) -> Unit,
    private val updateCurrentFileFn: (Path) -> Unit,
    private val processWithCountFn: (() -> Unit) -> Unit,
    private val incrementCurrentFn: () -> Unit,
    private val incrementProcessedFn: () -> Unit,
    private val incrementFailedFn: () -> Unit,
    private val printErrorFn: (Throwable) -> Unit,
    private val yieldFn: suspend () -> Unit
) {
    fun setTotal(total: Int) = setTotalFn(total)
    fun updateCurrentFile(file: Path) = updateCurrentFileFn(file)
    fun processWithCount(block: () -> Unit) = processWithCountFn(block)
    fun incrementCurrent() = incrementCurrentFn()
    fun incrementProcessed() = incrementProcessedFn()
    fun incrementFailed() = incrementFailedFn()
    fun printError(error: Throwable) = printErrorFn(error)
    suspend fun checkpoint() = yieldFn()
}
