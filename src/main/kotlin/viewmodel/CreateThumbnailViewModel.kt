package viewmodel

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
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

enum class CreateThumbnailOption {
    FIXED_SIZE, ASPECT_RATIO, RATIO
}

enum class ImageOutputFormat {
    JPEG, PNG, WEBP, ORIGINAL
}

class CreateThumbnailViewModel : ProcessViewModel(), KoinComponent {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY
    private val webpImageReader by inject<WebpImageReader>()
    private val imageIOReader by inject<ImageIOReader>()
    private val streamingGifWriter by inject<StreamingGifWriter>()

    private val _targetFormats = MutableStateFlow(setOf(Format.JPEG, Format.PNG, Format.GIF, Format.WEBP))
    val targetFormats = _targetFormats

    private val _imageOutputFormat = MutableStateFlow(ImageOutputFormat.JPEG)
    val imageOutputFormat = _imageOutputFormat

    private val _option = MutableStateFlow(CreateThumbnailOption.FIXED_SIZE)
    val option = _option

    private val _width = MutableStateFlow(200)
    val width = _width

    private val _height = MutableStateFlow(200)
    val height = _height

    private val _ratio = MutableStateFlow(50.0)
    val ratio = _ratio

    fun addTargetFormat(format: Format) {
        _targetFormats.value += format
    }

    fun removeTargetFormat(format: Format) {
        _targetFormats.value -= format
    }

    fun setImageOutputFormat(format: ImageOutputFormat) {
        _imageOutputFormat.value = format
    }

    fun setOption(option: CreateThumbnailOption) {
        _option.value = option
    }

    fun setWidth(width: Int) {
        _width.value = width
    }

    fun setHeight(height: Int) {
        _height.value = height
    }

    fun setRatio(ratio: Double) {
        _ratio.value = ratio
    }

    override fun onProcessClick() {
        process { basePath ->
            Files.list(basePath)
                .filter { file ->
                    targetFormats.value.any {
                        it.toExtension().any { extension ->
                            file.extension.equals(extension, true)
                        }
                    }
                }
                .forEach { file ->
                    val data = Files.readAllBytes(file)
                    val format = FormatDetector.detect(data).getOrNull()

                    if (!targetFormats.value.contains(format)) {
                        return@forEach
                    }

                    val outputExtension = when (imageOutputFormat.value) {
                        ImageOutputFormat.ORIGINAL -> file.extension
                        ImageOutputFormat.PNG -> "png"
                        ImageOutputFormat.JPEG -> "jpg"
                        ImageOutputFormat.WEBP -> "webp"
                    }

                    val thumbnailPath =
                        file.resolveSibling("${file.nameWithoutExtension}_thumbnail.${outputExtension}")

                    processWithCount {
                        createThumbnail(thumbnailPath, data, requireNotNull(format))
                    }
                }
        }
    }

    private fun createThumbnail(thumbnailPath: Path, data: ByteArray, format: Format) {
        when (format) {
            Format.GIF -> {
                val gif = AnimatedGifReader.read(ImageSource.of(data))

                if (imageOutputFormat.value == ImageOutputFormat.ORIGINAL) {
                    Files.newOutputStream(thumbnailPath).use { output ->
                        streamingGifWriter.prepareStream(output, gif.frames.first().type).use { gifStream ->
                            gif.frames.forEachIndexed { index, image ->
                                gifStream.writeFrame(toThumbnail(image), gif.getDelay(index))
                            }
                        }
                    }
                } else {
                    val image = toThumbnail(gif.frames.first())
                    image.output(getWriter(), thumbnailPath)
                }
            }

            else -> {
                val image = when (format) {
                    Format.WEBP -> webpImageReader.read(data)
                    else -> imageIOReader.read(data)
                }
                toThumbnail(image).output(getWriter(), thumbnailPath)
            }
        }
    }

    private fun toThumbnail(image: ImmutableImage): ImmutableImage {
        return when (option.value) {
            CreateThumbnailOption.FIXED_SIZE -> image.scaleTo(width.value, height.value)
            CreateThumbnailOption.ASPECT_RATIO -> image.scaleToWidth(width.value)
            CreateThumbnailOption.RATIO -> image.scaleToWidth((image.width * ratio.value / 100).toInt())
        }
    }

    private fun getWriter(): ImageWriter {
        return getSuitableImageWriter(
            when (imageOutputFormat.value) {
                ImageOutputFormat.JPEG -> Format.JPEG
                ImageOutputFormat.PNG -> Format.PNG
                ImageOutputFormat.WEBP -> Format.WEBP
                else -> throw IllegalArgumentException("Invalid image output format")
            }
        )
    }
}