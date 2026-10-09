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
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import infrastructure.image.canEncode
import infrastructure.image.getSuitableImageWriter
import infrastructure.image.maxDimension
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
            stream.filter { file -> Files.isRegularFile(file) }
                .filter { file ->
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
            runCatching {
                createThumbnail(
                    file = file,
                    targetFormats = targetFormats,
                    outputFormat = outputFormat,
                    option = option,
                    width = width,
                    height = height,
                    ratio = ratio
                )
            }
                .onSuccess { created ->
                    if (created) {
                        context.incrementProcessed()
                    }
                }
                .onFailure {
                    context.incrementFailed()
                    context.printError(it)
                }
            context.incrementCurrent()
        }
    }

    /** Creates a thumbnail next to [file]. Returns false when the file content is not one of [targetFormats]. */
    private fun createThumbnail(
        file: Path,
        targetFormats: Set<Format>,
        outputFormat: ImageOutputFormat,
        option: CreateThumbnailOption,
        width: Int,
        height: Int,
        ratio: Double
    ): Boolean {
        val data = Files.readAllBytes(file)
        val format = FormatDetector.detect(data).getOrNull()
        if (format == null || format !in targetFormats) {
            return false
        }

        val outputExtension = when (outputFormat) {
            ImageOutputFormat.ORIGINAL -> file.extension
            ImageOutputFormat.PNG -> "png"
            ImageOutputFormat.JPEG -> "jpg"
            ImageOutputFormat.WEBP -> "webp"
        }
        val thumbnailPath = file.resolveSibling("${file.nameWithoutExtension}_thumbnail.$outputExtension")
        val thumbnailFormat = outputFormat.toFormat(format)

        writeAtomically(thumbnailPath) { tempFile ->
            when (format) {
                Format.GIF -> {
                    val gif = AnimatedGifReader.read(ImageSource.of(data))

                    if (outputFormat == ImageOutputFormat.ORIGINAL) {
                        Files.newOutputStream(tempFile).use { output ->
                            streamingGifWriter.prepareStream(output, gif.frames.first().type).use { gifStream ->
                                gif.frames.forEachIndexed { index, image ->
                                    gifStream.writeFrame(toThumbnail(image, option, width, height, ratio), gif.getDelay(index))
                                }
                            }
                        }
                    } else {
                        val thumbnail = toThumbnail(gif.frames.first(), option, width, height, ratio)
                        writeThumbnail(file, thumbnail, thumbnailFormat, tempFile)
                    }
                }

                else -> {
                    val image = when (format) {
                        Format.WEBP -> webpImageReader.read(data)
                        else -> imageIOReader.read(data)
                    }
                    writeThumbnail(file, toThumbnail(image, option, width, height, ratio), thumbnailFormat, tempFile)
                }
            }
        }
        return true
    }

    private fun writeThumbnail(source: Path, thumbnail: ImmutableImage, format: Format, target: Path) {
        require(format.canEncode(thumbnail.width, thumbnail.height)) {
            val maxDimension = format.maxDimension()
            "The thumbnail of ${source.fileName} would be ${thumbnail.width} x ${thumbnail.height} pixels, " +
                "but $format supports up to $maxDimension x $maxDimension pixels"
        }
        thumbnail.output(getSuitableImageWriter(format), target)
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

    private fun ImageOutputFormat.toFormat(sourceFormat: Format): Format = when (this) {
        ImageOutputFormat.ORIGINAL -> sourceFormat
        ImageOutputFormat.JPEG -> Format.JPEG
        ImageOutputFormat.PNG -> Format.PNG
        ImageOutputFormat.WEBP -> Format.WEBP
    }
}
