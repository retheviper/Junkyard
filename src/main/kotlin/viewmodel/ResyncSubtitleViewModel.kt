package viewmodel

import kotlin.io.path.extension
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.component.inject
import service.ResyncSubtitleServiceStrategyFactory
import service.ResyncSubtitleType

class ResyncSubtitleViewModel : ProcessViewModel() {
    override val targetPickerType: TargetPickerType = TargetPickerType.FILE
    override val targetExtensions: List<String> = listOf("srt", "smi")
    private val strategyFactory: ResyncSubtitleServiceStrategyFactory by inject()

    private val _shiftMillis = MutableStateFlow(0)
    val shiftMillis = _shiftMillis

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
            val type = ResyncSubtitleType.fromString(basePath.extension)
            val strategy = strategyFactory.getStrategy(type)
            processWithCount {
                strategy.shiftSubtitle(basePath, shiftMillis.value)
            }
        }
    }
}