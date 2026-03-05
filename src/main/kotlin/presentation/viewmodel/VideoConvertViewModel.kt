package presentation.viewmodel

import application.model.VideoCodec
import application.model.VideoFormat
import application.usecase.VideoConvertUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject

class VideoConvertViewModel : ProcessViewModel() {
    private val videoConvertUseCase: VideoConvertUseCase by inject()
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    private val _targetFormat = MutableStateFlow(VideoFormat.ALL)
    val targetFormat = _targetFormat.asStateFlow()

    private val _videoCodec = MutableStateFlow(VideoCodec.H264)
    val videoCodec = _videoCodec.asStateFlow()

    private val _useHardwareEncoder = MutableStateFlow(true)
    val useHardwareEncoder = _useHardwareEncoder.asStateFlow()

    fun setTargetFormat(format: VideoFormat) {
        _targetFormat.value = format
    }

    fun setVideoCodec(codec: VideoCodec) {
        _videoCodec.value = codec
    }

    fun toggleUseHardwareEncoder() {
        _useHardwareEncoder.value = !_useHardwareEncoder.value
    }

    override fun onProcessClick() {
        process { basePath ->
            videoConvertUseCase.execute(
                basePath = basePath,
                targetFormat = targetFormat.value,
                videoCodec = videoCodec.value,
                useHardwareEncoder = useHardwareEncoder.value,
                context = createProcessingContext()
            )
        }
    }
}
