package infrastructure.di

import application.usecase.ArchiveDirectoriesUseCase
import application.usecase.ChangeExtensionUseCase
import application.usecase.CreateThumbnailUseCase
import application.usecase.ImageConvertUseCase
import application.usecase.RarToZipUseCase
import application.usecase.ResyncSubtitleUseCase
import application.usecase.VideoConvertUseCase
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import com.sksamuel.scrimage.webp.WebpWriter
import org.koin.dsl.module
import infrastructure.binary.BinaryBundleService
import domain.subtitle.ResyncSubtitleServiceStrategyFactory
import presentation.viewmodel.ArchiveViewModel
import presentation.viewmodel.ChangeExtensionViewModel
import presentation.viewmodel.CreateThumbnailViewModel
import presentation.viewmodel.ImageConvertViewModel
import presentation.viewmodel.RarToZipViewModel
import presentation.viewmodel.ResyncSubtitleViewModel
import presentation.viewmodel.VideoConvertViewModel

private val viewModelModules = module {
    single { ArchiveViewModel() }
    single { ChangeExtensionViewModel() }
    single { ImageConvertViewModel() }
    single { RarToZipViewModel() }
    single { CreateThumbnailViewModel() }
    single { ResyncSubtitleViewModel() }
    single { VideoConvertViewModel() }
}

private val imageModules = module {
    single { WebpImageReader() }
    single { ImageIOReader() }
    single { PngWriter() }
    single { JpegWriter.Default }
    single { GifWriter.Default }
    single { StreamingGifWriter() }
    single { Gif2WebpWriter.DEFAULT }
    single { WebpWriter.DEFAULT.withMultiThread() }
}

private val serviceModules = module {
    single { ResyncSubtitleServiceStrategyFactory() }
    single { BinaryBundleService() }
}

private val useCaseModules = module {
    single { ArchiveDirectoriesUseCase() }
    single { ChangeExtensionUseCase() }
    single { RarToZipUseCase() }
    single { ResyncSubtitleUseCase(get()) }
    single { VideoConvertUseCase(get()) }
    single { ImageConvertUseCase(get(), get(), get()) }
    single { CreateThumbnailUseCase(get(), get(), get()) }
}

val appModules = viewModelModules + imageModules + serviceModules + useCaseModules
