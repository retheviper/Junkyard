package presentation.viewmodel

import application.usecase.ArchiveDirectoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.inject

class ArchiveViewModel : ProcessViewModel() {
    private val archiveDirectoriesUseCase: ArchiveDirectoriesUseCase by inject()
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    private val _isParentDirectoryIncluded = MutableStateFlow(false)
    val isParentDirectoryIncluded = _isParentDirectoryIncluded.asStateFlow()

    fun toggleParentDirectory() {
        _isParentDirectoryIncluded.value = !_isParentDirectoryIncluded.value
    }

    override fun onProcessClick() {
        process { basePath ->
            archiveDirectoriesUseCase.execute(
                basePath = basePath,
                includeParentDirectory = isParentDirectoryIncluded.value,
                context = createProcessingContext()
            )
        }
    }
}
