package framework

import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import com.sksamuel.scrimage.webp.WebpWriter
import org.koin.dsl.module
import service.ResyncSubtitleServiceStrategyFactory
import viewmodel.ArchiveViewModel
import viewmodel.ChangeExtensionViewModel
import viewmodel.CreateThumbnailViewModel
import viewmodel.ImageConvertViewModel
import viewmodel.RarToZipViewModel
import viewmodel.ResyncSubtitleViewModel

private val viewModelModules = module {
    single { ArchiveViewModel() }
    single { ChangeExtensionViewModel() }
    single { ImageConvertViewModel() }
    single { RarToZipViewModel() }
    single { CreateThumbnailViewModel() }
    single { ResyncSubtitleViewModel() }
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
}

val appModules = viewModelModules + imageModules + serviceModules