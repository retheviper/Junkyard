package framework

import org.koin.dsl.module
import viewmodel.ArchiveViewModel
import viewmodel.ChangeExtensionViewModel
import viewmodel.ImageConvertViewModel
import viewmodel.RarToZipViewModel

private val viewModelModules = module {
    single { ArchiveViewModel() }
    single { ChangeExtensionViewModel() }
    single { ImageConvertViewModel() }
    single { RarToZipViewModel() }
}

val appModules = viewModelModules