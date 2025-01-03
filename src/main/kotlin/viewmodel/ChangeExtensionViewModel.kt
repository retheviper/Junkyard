package viewmodel

import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield

class ChangeExtensionViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    private val _ignoreCase = MutableStateFlow(false)
    val ignoreCase = _ignoreCase.asStateFlow()

    private val _fromExtension = MutableStateFlow("jpeg")
    val fromExtension = _fromExtension.asStateFlow()

    private val _toExtension = MutableStateFlow("jpg")
    val toExtension = _toExtension.asStateFlow()

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
        process { basePath ->
            val targets = Files.walk(basePath).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .filter { it != basePath }
                    .toList()
            }

            setTotal(targets.size)

            targets.forEach {
                yield()
                val newFileName = "${it.nameWithoutExtension}.${toExtension.value}"
                processWithCount {
                    updateCurrentFile(it)
                    Files.move(it, it.resolveSibling(newFileName))
                }
            }
        }
    }
}