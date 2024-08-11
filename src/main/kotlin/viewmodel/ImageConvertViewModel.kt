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
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.flow.MutableStateFlow
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
    val fromFormat: MutableStateFlow<Format> = _fromFormat

    private val _toFormat = MutableStateFlow(Format.WEBP)
    val toFormat: MutableStateFlow<Format> = _toFormat

    private val _includeArchiveFiles = MutableStateFlow(false)
    val includeArchiveFiles: MutableStateFlow<Boolean> = _includeArchiveFiles

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
            Files.walk(basePath)
                .filter { file ->
                    fromFormat.value.toExtension().any {
                        file.extension.equals(it, true)
                    } || (includeArchiveFiles.value && ArchiveFormat.entries.any {
                        file.extension.equals(it.name, true)
                    })
                }
                .forEach { file ->
                    if (includeArchiveFiles.value && ArchiveFormat.entries.any {
                            file.extension.equals(it.name, true)
                        }) {
                        handleArchiveFile(file)
                    } else {
                        runCatching {
                            handleImageFile(file)
                        }.onSuccess {
                            Files.deleteIfExists(file)
                            incrementProcessed()
                        }.onFailure {
                            incrementFailed()
                        }
                    }
                }
        }
    }

    private fun handleArchiveFile(zipFilePath: Path) {
        val tempPath = Files.createTempDirectory(UUID.randomUUID().toString())

        runCatching {
            ZipInputStream(Files.newInputStream(zipFilePath)).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    val entryPath = tempPath.resolve(entry.name)
                    Files.createDirectories(entryPath.parent)
                    Files.copy(zipInputStream, entryPath)
                    entry = zipInputStream.nextEntry
                }
            }
        }.onFailure {
            incrementFailed()
            tempPath.toFile().deleteRecursively()
            return
        }

        runCatching {
            Files.walk(tempPath)
                .filter { Files.isRegularFile(it) }
                .forEach { file ->
                    runCatching {
                        handleImageFile(file)
                    }.onSuccess {
                        Files.deleteIfExists(file)
                    }.onFailure {
                        throw it
                    }
                }
        }.onFailure {
            incrementFailed()
            tempPath.toFile().deleteRecursively()
            return
        }

        runCatching {
            zipFiles(tempPath, zipFilePath)
        }.onSuccess {
            incrementProcessed()
        }.onFailure {
            incrementFailed()
        }

        tempPath.toFile().deleteRecursively()
    }

    private fun handleImageFile(filePath: Path) {
        val data = Files.readAllBytes(filePath)
        val format = FormatDetector.detect(data).getOrNull()

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
}