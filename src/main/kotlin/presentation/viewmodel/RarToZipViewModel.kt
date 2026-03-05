package presentation.viewmodel

import application.usecase.RarToZipUseCase
import org.koin.core.component.inject

class RarToZipViewModel : ProcessViewModel() {
    private val rarToZipUseCase: RarToZipUseCase by inject()
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    override fun onProcessClick() {
        process { basePath ->
            rarToZipUseCase.execute(
                basePath = basePath,
                context = createProcessingContext()
            )
        }
    }
}
