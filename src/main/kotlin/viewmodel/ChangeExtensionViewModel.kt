package viewmodel

import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlinx.coroutines.flow.MutableStateFlow

class ChangeExtensionViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

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
        process { basePath ->
            Files.walk(basePath)
                .filter { it.extension.equals(fromExtension.value, ignoreCase.value) }
                .forEach {
                    val newFileName = it.nameWithoutExtension + toExtension.value
                    processWithCount {
                        Files.move(it, it.resolveSibling(newFileName))
                    }
                }
        }
    }
}