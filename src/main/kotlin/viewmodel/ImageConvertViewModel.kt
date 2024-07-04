package viewmodel

import androidx.lifecycle.viewModelScope
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import com.sksamuel.scrimage.webp.WebpWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageConvertViewModel : ProcessViewModel() {
    private val webpImageReader = WebpImageReader()
    private val imageIOReader = ImageIOReader()
    private val gifWriter = GifWriter.Default
    private val pngWriter = PngWriter()
    private val jpegWriter = JpegWriter.Default
    private val gif2WebpWriter = Gif2WebpWriter.DEFAULT
    private val webpWriter = WebpWriter.DEFAULT.withMultiThread()

    private val _path: MutableStateFlow<Path?> = MutableStateFlow(null)
    val path: StateFlow<Path?> = _path

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting

    fun setPath(path: Path) {
        _path.value = path
    }

    fun onConvertClick(fromFormat: Format, toFormat: Format) {
        val basePath = path.value ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _isConverting.value = true
                try {
                    Files.walk(basePath)
                        .filter { file ->
                            fromFormat.toExtension().any {
                                file.extension.equals(it, true)
                            }
                        }
                        .forEach { file ->
                            val data = Files.readAllBytes(file)
                            if (fromFormat != FormatDetector.detect(data).getOrNull()) {
                                return@forEach
                            }

                            val convertedFilePath =
                                file.resolveSibling("${file.nameWithoutExtension}.${toFormat.toExtension().first()}")
                            runCatching {
                                convertImage(convertedFilePath, data, fromFormat, toFormat)
                            }.onSuccess {
                                Files.deleteIfExists(file)
                                incrementProcessed()
                            }.onFailure {
                                incrementFailed()
                            }
                        }
                } finally {
                    _isConverting.value = false
                }
            }
        }
    }

    private fun Format.toExtension() = when (this) {
        Format.JPEG -> listOf("jpg", "jpeg")
        Format.PNG -> listOf("png")
        Format.WEBP -> listOf("webp")
        Format.GIF -> listOf("gif")
    }

    private fun convertImage(filePath: Path, data: ByteArray, fromFormat: Format, toFormat: Format) {
        when (fromFormat) {
            Format.GIF -> {
                val image = AnimatedGifReader.read(ImageSource.of(data))
                when (toFormat) {
                    Format.WEBP -> image.output(gif2WebpWriter, filePath)

                    else -> {
                        val writer = when (toFormat) {
                            Format.JPEG -> jpegWriter
                            Format.PNG -> pngWriter
                            else -> throw IllegalArgumentException("Unsupported format")
                        }
                        image.frames.first().output(writer, filePath)
                    }
                }
            }

            Format.WEBP -> {
                val image = webpImageReader.read(data)
                writeImage(toFormat, image, filePath)
            }

            else -> {
                val image = imageIOReader.read(data)
                writeImage(toFormat, image, filePath)
            }
        }
    }

    private fun writeImage(toFormat: Format, image: ImmutableImage, filePath: Path) {
        val writer = when (toFormat) {
            Format.WEBP -> webpWriter
            Format.JPEG -> jpegWriter
            Format.PNG -> pngWriter
            Format.GIF -> gifWriter
        }
        image.output(writer, filePath)
    }
}