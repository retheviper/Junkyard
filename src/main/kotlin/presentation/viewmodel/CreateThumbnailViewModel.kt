package presentation.viewmodel

import application.model.CreateThumbnailOption
import application.model.ImageOutputFormat
import application.usecase.CreateThumbnailUseCase
import com.sksamuel.scrimage.format.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject

class CreateThumbnailViewModel : ProcessViewModel() {
    private val createThumbnailUseCase: CreateThumbnailUseCase by inject()
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    private val _targetFormats = MutableStateFlow(setOf(Format.JPEG, Format.PNG, Format.GIF, Format.WEBP))
    val targetFormats = _targetFormats.asStateFlow()

    private val _imageOutputFormat = MutableStateFlow(ImageOutputFormat.JPEG)
    val imageOutputFormat = _imageOutputFormat.asStateFlow()

    private val _option = MutableStateFlow(CreateThumbnailOption.FIXED_SIZE)
    val option = _option.asStateFlow()

    private val _width = MutableStateFlow(200)
    val width = _width.asStateFlow()

    private val _height = MutableStateFlow(200)
    val height = _height.asStateFlow()

    private val _ratio = MutableStateFlow(50.0)
    val ratio = _ratio.asStateFlow()

    fun addTargetFormat(format: Format) {
        _targetFormats.value += format
    }

    fun removeTargetFormat(format: Format) {
        _targetFormats.value -= format
    }

    fun setImageOutputFormat(format: ImageOutputFormat) {
        _imageOutputFormat.value = format
    }

    fun setOption(option: CreateThumbnailOption) {
        _option.value = option
    }

    fun setWidth(width: Int) {
        _width.value = width
    }

    fun setHeight(height: Int) {
        _height.value = height
    }

    fun setRatio(ratio: Double) {
        _ratio.value = ratio
    }

    override fun onProcessClick() {
        process { basePath ->
            createThumbnailUseCase.execute(
                basePath = basePath,
                targetFormats = targetFormats.value,
                outputFormat = imageOutputFormat.value,
                option = option.value,
                width = width.value,
                height = height.value,
                ratio = ratio.value,
                context = createProcessingContext()
            )
        }
    }
}
