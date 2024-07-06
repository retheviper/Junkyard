package viewmodel

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import framework.getSuitableImageWriter
import framework.toExtension
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImageConvertViewModel : ProcessViewModel(), KoinComponent {
    private val webpImageReader by inject<WebpImageReader>()
    private val imageIOReader by inject<ImageIOReader>()
    private val gif2WebpWriter by inject<Gif2WebpWriter>()

    private val _fromFormat = MutableStateFlow(Format.JPEG)
    val fromFormat: MutableStateFlow<Format> = _fromFormat

    private val _toFormat = MutableStateFlow(Format.WEBP)
    val toFormat: MutableStateFlow<Format> = _toFormat

    fun setFromFormat(format: Format) {
        _fromFormat.value = format
    }

    fun setToFormat(format: Format) {
        _toFormat.value = format
    }

    override fun onProcessClick() {
        process { basePath ->
            Files.walk(basePath)
                .filter { file ->
                    fromFormat.value.toExtension().any {
                        file.extension.equals(it, true)
                    }
                }
                .forEach { file ->
                    val data = Files.readAllBytes(file)

                    if (fromFormat.value != FormatDetector.detect(data).getOrNull()) {
                        return@forEach
                    }

                    val convertedFilePath = file.resolveSibling(
                        "${file.nameWithoutExtension}.${toFormat.value.toExtension().first()}"
                    )

                    runCatching {
                        convertImage(convertedFilePath, data, fromFormat.value, toFormat.value)
                    }.onSuccess {
                        Files.deleteIfExists(file)
                        incrementProcessed()
                    }.onFailure {
                        incrementFailed()
                    }
                }
        }
    }

    private fun convertImage(filePath: Path, data: ByteArray, fromFormat: Format, toFormat: Format) {
        when (fromFormat) {
            Format.GIF -> {
                val image = AnimatedGifReader.read(ImageSource.of(data))
                when (toFormat) {
                    Format.WEBP -> image.output(gif2WebpWriter, filePath)

                    else -> {
                        val writer = getSuitableImageWriter(toFormat)
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
        val writer = getSuitableImageWriter(toFormat)
        image.output(writer, filePath)
    }
}