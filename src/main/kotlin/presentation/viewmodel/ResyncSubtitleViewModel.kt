package presentation.viewmodel

import application.usecase.ResyncSubtitleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject

class ResyncSubtitleViewModel : ProcessViewModel() {
    private val resyncSubtitleUseCase: ResyncSubtitleUseCase by inject()
    override val targetPickerType: TargetPickerType = TargetPickerType.FILE
    override val targetExtensions: List<String> = listOf("srt", "smi")

    private val _shiftMillis = MutableStateFlow(0)
    val shiftMillis = _shiftMillis.asStateFlow()

    fun setShiftMillis(value: Int) {
        _shiftMillis.value = value
    }

    fun increaseShiftMillis() {
        _shiftMillis.value++
    }

    fun decreaseShiftMillis() {
        _shiftMillis.value--
    }

    override fun onProcessClick() {
        process { basePath ->
            resyncSubtitleUseCase.execute(
                basePath = basePath,
                shiftMillis = shiftMillis.value,
                context = createProcessingContext()
            )
        }
    }
}
