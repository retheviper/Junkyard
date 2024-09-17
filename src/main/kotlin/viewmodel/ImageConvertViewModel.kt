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
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.jvm.optionals.getOrElse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class ArchiveFormat {
    ZIP, CBZ
}

class ImageConvertViewModel : ProcessViewModel(), KoinComponent {
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY
    private val webpImageReader by inject<WebpImageReader>()
    private val imageIOReader by inject<ImageIOReader>()
    private val gif2WebpWriter by inject<Gif2WebpWriter>()

    private val _fromFormat = MutableStateFlow(Format.JPEG)
    val fromFormat = _fromFormat.asStateFlow()

    private val _toFormat = MutableStateFlow(Format.WEBP)
    val toFormat = _toFormat.asStateFlow()

    private val _includeArchiveFiles = MutableStateFlow(false)
    val includeArchiveFiles = _includeArchiveFiles.asStateFlow()

    fun setFromFormat(format: Format) {
        _fromFormat.value = format
    }

    fun setToFormat(format: Format) {
        _toFormat.value = format
    }

    fun toggleIncludeArchiveFiles() {
        _includeArchiveFiles.value = !_includeArchiveFiles.value
    }

    override fun onProcessClick() {
        process { basePath ->
            val targets = Files.walk(basePath)
                .filter { file ->
                    fromFormat.value.toExtension().any {
                        file.extension.equals(it, true)
                    } || (_includeArchiveFiles.value && file.isArchiveFile)
                }
                .toList()

            setTotal(targets.size)

            targets.forEach { file ->
                yield()
                processWithCount {
                    if (_includeArchiveFiles.value && file.isArchiveFile) {
                        val tempPath = Files.createTempDirectory(UUID.randomUUID().toString())
                        runCatching { handleArchiveFile(file, tempPath) }
                            .exceptionOrNull()
                            .also { tempPath.toFile().deleteRecursively() }
                    } else {
                        handleImageFile(file)
                        Files.deleteIfExists(file)
                    }
                }
            }
        }
    }

    private fun handleArchiveFile(zipFilePath: Path, tempPath: Path) {
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

        Files.walk(tempPath)
            .filter { Files.isRegularFile(it) }
            .forEach { file ->
                handleImageFile(file)
                Files.deleteIfExists(file)
            }

        zipFiles(tempPath, zipFilePath)
    }

    private fun handleImageFile(filePath: Path) {
        val data = Files.readAllBytes(filePath)
        val format = FormatDetector.detect(data).getOrElse { return }

        if (fromFormat.value != format) {
            return
        }

        val convertedFilePath = filePath.resolveSibling(
            "${filePath.nameWithoutExtension}.${toFormat.value.toExtension().first()}"
        )

        convertImage(convertedFilePath, data, fromFormat.value, toFormat.value)
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

    private val Path.isArchiveFile: Boolean
        get() = ArchiveFormat.entries.any { extension.equals(it.name, true) }
}