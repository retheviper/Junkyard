package presentation.viewmodel

import application.usecase.ChangeExtensionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject

class ChangeExtensionViewModel : ProcessViewModel() {
    private val changeExtensionUseCase: ChangeExtensionUseCase by inject()
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
            changeExtensionUseCase.execute(
                basePath = basePath,
                toExtension = toExtension.value,
                context = createProcessingContext()
            )
        }
    }
}
