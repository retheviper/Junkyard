package application.usecase

import application.model.ArchiveFormat
import application.model.ImageFromFormat
import application.model.OversizedImage
import application.processing.ProcessingContext
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import infrastructure.image.ImageSize
import infrastructure.image.canEncode
import infrastructure.image.getSuitableImageWriter
import infrastructure.image.maxDimension
import infrastructure.image.readImageSize
import infrastructure.image.toExtension
import java.io.InputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrNull
import org.koin.core.component.KoinComponent

class ImageConvertUseCase(
    private val webpImageReader: WebpImageReader,
    private val imageIOReader: ImageIOReader,
    private val gif2WebpWriter: Gif2WebpWriter
) : KoinComponent {
    private val imageExtensions = ImageFromFormat.entries
        .flatMap { it.extensions }
        .map { it.lowercase() }
        .toSet()

    private val archiveExtensions = ArchiveFormat.entries
        .map { it.name.lowercase() }
        .toSet()

    fun supportedTargetExtensions(): List<String> = buildList {
        addAll(ImageFromFormat.entries.flatMap { it.extensions })
        addAll(ArchiveFormat.entries.map { it.name.lowercase() })
    }

    /**
     * Lists the images [execute] would convert but [toFormat] cannot encode because of its size limit.
     * Only image headers are read, so this is cheap compared with the conversion itself.
     */
    suspend fun findOversizedImages(
        basePath: Path,
        fromFormat: ImageFromFormat,
        toFormat: Format,
        includeArchiveFiles: Boolean,
        context: ProcessingContext
    ): List<OversizedImage> {
        if (toFormat.maxDimension() == null) {
            return emptyList()
        }

        val oversizedImages = mutableListOf<OversizedImage>()
        collectTargets(basePath, includeArchiveFiles).forEach { file ->
            context.checkpoint()
            context.updateCurrentFile(file)
            val displayPath = if (file == basePath) file.fileName.toString() else basePath.relativize(file).toString()
            // Files that cannot be inspected are left to the conversion, which reports them as failures.
            runCatching {
                if (includeArchiveFiles && file.isArchiveFile) {
                    ZipFile(file.toFile()).use { zipFile ->
                        zipFile.entries().asSequence()
                            .filterNot { it.isDirectory }
                            .forEach { entry ->
                                val size = zipFile.getInputStream(entry).use { input ->
                                    readConversionSize(entry.name.substringAfterLast('/'), input, fromFormat, toFormat)
                                }
                                if (size != null && !toFormat.canEncode(size.width, size.height)) {
                                    oversizedImages += OversizedImage("$displayPath/${entry.name}", size)
                                }
                            }
                    }
                } else {
                    val size = Files.newInputStream(file).use { input ->
                        readConversionSize(file.fileName.toString(), input, fromFormat, toFormat)
                    }
                    if (size != null && !toFormat.canEncode(size.width, size.height)) {
                        oversizedImages += OversizedImage(displayPath, size)
                    }
                }
            }
        }
        return oversizedImages
    }

    suspend fun execute(
        basePath: Path,
        fromFormat: ImageFromFormat,
        toFormat: Format,
        includeArchiveFiles: Boolean,
        context: ProcessingContext
    ) {
        val targets = collectTargets(basePath, includeArchiveFiles)
        context.setTotal(targets.size)

        targets.forEach { file ->
            context.checkpoint()
            context.updateCurrentFile(file)
            if (includeArchiveFiles && file.isArchiveFile) {
                val tempPath = Files.createTempDirectory(UUID.randomUUID().toString())
                runCatching { handleArchiveFile(file, tempPath, fromFormat, toFormat, context) }
                    .onSuccess { convertedCount ->
                        if (convertedCount > 0) {
                            context.incrementProcessed()
                        }
                    }
                    .onFailure {
                        context.incrementFailed()
                        context.printError(it)
                    }
                    .also { tempPath.toFile().deleteRecursively() }
            } else {
                runCatching { handleImageFile(file, fromFormat, toFormat) }
                    .onSuccess { converted ->
                        if (converted) {
                            context.incrementProcessed()
                        }
                    }
                    .onFailure {
                        context.incrementFailed()
                        context.printError(it)
                    }
            }
            context.incrementCurrent()
        }
    }

    private fun collectTargets(basePath: Path, includeArchiveFiles: Boolean): List<Path> {
        return if (Files.isRegularFile(basePath)) {
            listOf(basePath).filter { isSupportedTarget(it, includeArchiveFiles) }
        } else {
            Files.walk(basePath).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .filter { it != basePath }
                    .filter { isSupportedTarget(it, includeArchiveFiles) }
                    .toList()
            }
        }
    }

    private fun handleArchiveFile(
        zipFilePath: Path,
        tempPath: Path,
        fromFormat: ImageFromFormat,
        toFormat: Format,
        context: ProcessingContext
    ): Int {
        newZipInputStream(zipFilePath).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val entryPath = tempPath.resolve(entry.name).normalize()
                if (!entryPath.startsWith(tempPath.normalize())) {
                    throw IllegalArgumentException("Zip entry escapes target directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    Files.createDirectories(entryPath)
                } else {
                    entryPath.parent?.let { Files.createDirectories(it) }
                    Files.copy(zipInputStream, entryPath)
                }
                entry = zipInputStream.nextEntry
            }
        }

        // Collect before converting: the conversion creates files in the tree being walked.
        val files = Files.walk(tempPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }.toList()
        }

        var convertedCount = 0
        files.forEach { file ->
            runCatching { handleImageFile(file, fromFormat, toFormat) }
                .onSuccess { converted ->
                    if (converted) {
                        convertedCount++
                    }
                }
                .onFailure {
                    context.incrementFailed()
                    context.printError(it)
                }
        }

        // Rewriting an archive is expensive, so archives without converted images are left untouched.
        if (convertedCount > 0) {
            zipFiles(tempPath, zipFilePath)
        }
        return convertedCount
    }

    /** Converts [filePath] to [toFormat] when it is a target, replacing the original. Returns whether it was converted. */
    private fun handleImageFile(filePath: Path, fromFormat: ImageFromFormat, toFormat: Format): Boolean {
        val extension = filePath.extension.lowercase()
        if (!isConvertibleExtension(extension, fromFormat)) {
            return false
        }

        // AVIF has no scrimage format and is always decoded through ImageIO.
        val sourceFormat = if (extension == AVIF_EXTENSION) {
            null
        } else {
            // Detecting the format from the header avoids reading whole files that are skipped anyway.
            val detectedFormat = Files.newInputStream(filePath).use { FormatDetector.detect(it) }.getOrNull()
            if (detectedFormat == null || !shouldConvert(detectedFormat, fromFormat, toFormat)) {
                return false
            }
            detectedFormat
        }

        val convertedFilePath = filePath.resolveSibling(
            "${filePath.nameWithoutExtension}.${toFormat.toExtension().first()}"
        )
        // On case-insensitive file systems "a.PNG" and "a.png" are the same file, which is replaced in place.
        val replacesOriginal = isSameExistingFile(filePath, convertedFilePath)
        if (!replacesOriginal && Files.exists(convertedFilePath)) {
            throw FileAlreadyExistsException(filePath.toString(), convertedFilePath.toString(), "converted file already exists")
        }

        val data = Files.readAllBytes(filePath)
        writeAtomically(convertedFilePath) { tempFile ->
            convertImage(filePath, tempFile, data, sourceFormat, toFormat)
        }
        if (!replacesOriginal) {
            Files.deleteIfExists(filePath)
        }
        return true
    }

    private fun convertImage(source: Path, target: Path, data: ByteArray, sourceFormat: Format?, toFormat: Format) {
        if (sourceFormat == Format.GIF) {
            val gif = AnimatedGifReader.read(ImageSource.of(data))
            requireEncodable(source, toFormat, ImageSize(gif.dimensions.width, gif.dimensions.height))
            when (toFormat) {
                Format.WEBP -> gif.output(gif2WebpWriter, target)
                else -> gif.frames.first().output(getSuitableImageWriter(toFormat), target)
            }
            return
        }

        val image = when (sourceFormat) {
            Format.WEBP -> webpImageReader.read(data)
            else -> imageIOReader.read(data)
        }
        requireEncodable(source, toFormat, ImageSize(image.width, image.height))
        image.output(getSuitableImageWriter(toFormat), target)
    }

    private fun requireEncodable(source: Path, format: Format, size: ImageSize) {
        require(format.canEncode(size.width, size.height)) {
            val maxDimension = format.maxDimension()
            "${source.fileName} is ${size.width} x ${size.height} pixels, " +
                "but $format supports up to $maxDimension x $maxDimension pixels"
        }
    }

    /** Returns the image size when [handleImageFile] would convert a file named [fileName], or null otherwise. */
    private fun readConversionSize(
        fileName: String,
        input: InputStream,
        fromFormat: ImageFromFormat,
        toFormat: Format
    ): ImageSize? {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (!isConvertibleExtension(extension, fromFormat)) {
            return null
        }

        val bufferedInput = input.buffered()
        if (extension != AVIF_EXTENSION) {
            bufferedInput.mark(FORMAT_HEADER_READ_LIMIT)
            val detectedFormat = FormatDetector.detect(bufferedInput).getOrNull()
            if (detectedFormat == null || !shouldConvert(detectedFormat, fromFormat, toFormat)) {
                return null
            }
            bufferedInput.reset()
        }
        return readImageSize(bufferedInput)
    }

    private fun isConvertibleExtension(extension: String, fromFormat: ImageFromFormat): Boolean =
        extension in imageExtensions && fromFormat.matchesExtension(extension)

    private fun shouldConvert(detectedFormat: Format, fromFormat: ImageFromFormat, toFormat: Format): Boolean =
        fromFormat.matches(detectedFormat) && detectedFormat != toFormat

    private val Path.isArchiveFile: Boolean
        get() = extension.lowercase() in archiveExtensions

    private fun isSupportedTarget(path: Path, includeArchiveFiles: Boolean): Boolean {
        val extension = path.extension.lowercase()
        return when {
            extension in imageExtensions -> true
            extension in archiveExtensions -> includeArchiveFiles
            else -> false
        }
    }

    private companion object {
        const val AVIF_EXTENSION = "avif"

        // FormatDetector reads the first 12 bytes; the limit leaves headroom for reset().
        const val FORMAT_HEADER_READ_LIMIT = 64
    }
}
