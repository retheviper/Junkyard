package framework

import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import com.sksamuel.scrimage.webp.WebpWriter
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

private val imageConverterModules = module {
    single { WebpImageReader() }
    single { ImageIOReader() }
    single { GifWriter.Default }
    single { Gif2WebpWriter.DEFAULT }

    single { WebpWriter.DEFAULT.withMultiThread() }
}

val appModules = viewModelModules