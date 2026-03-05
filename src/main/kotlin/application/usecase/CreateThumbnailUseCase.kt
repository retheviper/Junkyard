package application.usecase

import application.model.CreateThumbnailOption
import application.model.ImageOutputFormat
import application.processing.ProcessingContext
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import infrastructure.image.getSuitableImageWriter
import infrastructure.image.toExtension
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrNull
import org.koin.core.component.KoinComponent

class CreateThumbnailUseCase(
    private val webpImageReader: WebpImageReader,
    private val imageIOReader: ImageIOReader,
    private val streamingGifWriter: StreamingGifWriter
) : KoinComponent {
    suspend fun execute(
        basePath: Path,
        targetFormats: Set<Format>,
        outputFormat: ImageOutputFormat,
        option: CreateThumbnailOption,
        width: Int,
        height: Int,
        ratio: Double,
        context: ProcessingContext
    ) {
        val targets = Files.walk(basePath).use { stream ->
            stream.filter { file ->
                targetFormats.any { format ->
                    format.toExtension().any { extension ->
                        file.extension.equals(extension, true)
                    }
                }
            }.toList()
        }

        context.setTotal(targets.size)

        targets.forEach { file ->
            context.checkpoint()
            context.updateCurrentFile(file)
            val data = Files.readAllBytes(file)
            val format = FormatDetector.detect(data).getOrNull()

            if (!targetFormats.contains(format)) {
                return@forEach
            }

            val outputExtension = when (outputFormat) {
                ImageOutputFormat.ORIGINAL -> file.extension
                ImageOutputFormat.PNG -> "png"
                ImageOutputFormat.JPEG -> "jpg"
                ImageOutputFormat.WEBP -> "webp"
            }

            val thumbnailPath = file.resolveSibling("${file.nameWithoutExtension}_thumbnail.$outputExtension")

            context.processWithCount {
                createThumbnail(
                    thumbnailPath = thumbnailPath,
                    data = data,
                    format = requireNotNull(format),
                    outputFormat = outputFormat,
                    option = option,
                    width = width,
                    height = height,
                    ratio = ratio
                )
            }
        }
    }

    private fun createThumbnail(
        thumbnailPath: Path,
        data: ByteArray,
        format: Format,
        outputFormat: ImageOutputFormat,
        option: CreateThumbnailOption,
        width: Int,
        height: Int,
        ratio: Double
    ) {
        when (format) {
            Format.GIF -> {
                val gif = AnimatedGifReader.read(ImageSource.of(data))

                if (outputFormat == ImageOutputFormat.ORIGINAL) {
                    Files.newOutputStream(thumbnailPath).use { output ->
                        streamingGifWriter.prepareStream(output, gif.frames.first().type).use { gifStream ->
                            gif.frames.forEachIndexed { index, image ->
                                gifStream.writeFrame(toThumbnail(image, option, width, height, ratio), gif.getDelay(index))
                            }
                        }
                    }
                } else {
                    val image = toThumbnail(gif.frames.first(), option, width, height, ratio)
                    image.output(getWriter(outputFormat), thumbnailPath)
                }
            }

            else -> {
                val image = when (format) {
                    Format.WEBP -> webpImageReader.read(data)
                    else -> imageIOReader.read(data)
                }
                toThumbnail(image, option, width, height, ratio).output(getWriter(outputFormat), thumbnailPath)
            }
        }
    }

    private fun toThumbnail(
        image: ImmutableImage,
        option: CreateThumbnailOption,
        width: Int,
        height: Int,
        ratio: Double
    ): ImmutableImage {
        return when (option) {
            CreateThumbnailOption.FIXED_SIZE -> image.scaleTo(width, height)
            CreateThumbnailOption.ASPECT_RATIO -> image.scaleToWidth(width)
            CreateThumbnailOption.RATIO -> image.scaleToWidth((image.width * ratio / 100).toInt())
        }
    }

    private fun getWriter(outputFormat: ImageOutputFormat): ImageWriter {
        return getSuitableImageWriter(
            when (outputFormat) {
                ImageOutputFormat.JPEG -> Format.JPEG
                ImageOutputFormat.PNG -> Format.PNG
                ImageOutputFormat.WEBP -> Format.WEBP
                else -> throw IllegalArgumentException("Invalid image output format")
            }
        )
    }
}
