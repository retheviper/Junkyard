package presentation.viewmodel

import application.model.ImageFromFormat
import application.usecase.ImageConvertUseCase
import com.sksamuel.scrimage.format.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject

class ImageConvertViewModel : ProcessViewModel() {
    private val imageConvertUseCase: ImageConvertUseCase by inject()
    override val targetPickerType: TargetPickerType = TargetPickerType.BOTH
    override val targetExtensions: List<String> = imageConvertUseCase.supportedTargetExtensions()

    private val _fromFormat = MutableStateFlow(ImageFromFormat.ALL)
    val fromFormat = _fromFormat.asStateFlow()

    private val _toFormat = MutableStateFlow(Format.WEBP)
    val toFormat = _toFormat.asStateFlow()

    private val _includeArchiveFiles = MutableStateFlow(false)
    val includeArchiveFiles = _includeArchiveFiles.asStateFlow()

    fun setFromFormat(format: ImageFromFormat) {
        _fromFormat.value = format
    }

    fun setToFormat(format: Format) {
        _toFormat.value = format
    }

    fun toggleIncludeArchiveFiles() {
        _includeArchiveFiles.value = !_includeArchiveFiles.value
    }

    override fun onProcessClick() {
        process { basePath ->
            imageConvertUseCase.execute(
                basePath = basePath,
                fromFormat = fromFormat.value,
                toFormat = toFormat.value,
                includeArchiveFiles = includeArchiveFiles.value,
                context = createProcessingContext()
            )
        }
    }
}
