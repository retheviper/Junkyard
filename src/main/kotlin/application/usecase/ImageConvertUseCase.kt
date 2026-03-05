package application.usecase

import application.model.ArchiveFormat
import application.model.ImageFromFormat
import application.processing.ProcessingContext
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import infrastructure.image.getSuitableImageWriter
import infrastructure.image.toExtension
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrElse
import org.koin.core.component.KoinComponent

class ImageConvertUseCase(
    private val webpImageReader: WebpImageReader,
    private val imageIOReader: ImageIOReader,
    private val gif2WebpWriter: Gif2WebpWriter
) : KoinComponent {
    private val imageExtensions = Format.entries
        .flatMap { it.toExtension() }
        .map { it.lowercase() }
        .toSet()

    private val archiveExtensions = ArchiveFormat.entries
        .map { it.name.lowercase() }
        .toSet()

    private val extensionsByFormat = Format.entries.associateWith { format ->
        format.toExtension().map { it.lowercase() }.toSet()
    }

    fun supportedTargetExtensions(): List<String> = buildList {
        addAll(Format.entries.flatMap { it.toExtension() })
        addAll(ArchiveFormat.entries.map { it.name.lowercase() })
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
                            Files.deleteIfExists(file)
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
        var convertedCount = 0
        ZipInputStream(Files.newInputStream(zipFilePath)).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val entryPath = tempPath.resolve(entry.name)
                if (entry.isDirectory) {
                    Files.createDirectories(entryPath)
                } else {
                    Files.copy(zipInputStream, entryPath)
                }
                entry = zipInputStream.nextEntry
            }
        }

        Files.walk(tempPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .forEach { file ->
                    runCatching { handleImageFile(file, fromFormat, toFormat) }
                        .onSuccess { converted ->
                            if (converted) {
                                Files.deleteIfExists(file)
                                convertedCount++
                            }
                        }
                        .onFailure {
                            context.incrementFailed()
                            context.printError(it)
                        }
                }
        }

        zipFiles(tempPath, zipFilePath)
        return convertedCount
    }

    private fun handleImageFile(filePath: Path, fromFormat: ImageFromFormat, toFormat: Format): Boolean {
        val extension = filePath.extension.lowercase()
        if (extension !in imageExtensions) {
            return false
        }

        val selectedFromFormat = fromFormat.format
        if (selectedFromFormat != null && extension !in extensionsByFormat.getValue(selectedFromFormat)) {
            return false
        }

        val data = Files.readAllBytes(filePath)
        val detectedFormat = FormatDetector.detect(data).getOrElse { return false }

        if (!fromFormat.matches(detectedFormat) || detectedFormat == toFormat) {
            return false
        }

        val convertedFilePath = filePath.resolveSibling(
            "${filePath.nameWithoutExtension}.${toFormat.toExtension().first()}"
        )

        convertImage(convertedFilePath, data, detectedFormat, toFormat)
        return true
    }

    private fun convertImage(filePath: Path, data: ByteArray, fromFormat: Format, toFormat: Format) {
        when (fromFormat) {
            Format.GIF -> {
                val image = AnimatedGifReader.read(ImageSource.of(data))
                when (toFormat) {
                    Format.WEBP -> image.output(gif2WebpWriter, filePath)
                    else -> image.frames.first().output(getSuitableImageWriter(toFormat), filePath)
                }
            }

            Format.WEBP -> writeImage(toFormat, webpImageReader.read(data), filePath)
            else -> writeImage(toFormat, imageIOReader.read(data), filePath)
        }
    }

    private fun writeImage(toFormat: Format, image: ImmutableImage, filePath: Path) {
        image.output(getSuitableImageWriter(toFormat), filePath)
    }

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
}
