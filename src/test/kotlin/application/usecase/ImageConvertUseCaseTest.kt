package application.usecase

import application.model.ImageFromFormat
import application.model.OversizedImage
import application.processing.ProcessingContext
import application.processing.RecordingProcessingContext
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpImageReader
import com.sksamuel.scrimage.webp.WebpWriter
import infrastructure.image.ImageSize
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ImageConvertUseCaseTest {
    private val useCase = ImageConvertUseCase(WebpImageReader(), ImageIOReader(), Gif2WebpWriter.DEFAULT)

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `converts an AVIF input to PNG`() = withTempDirectory { directory ->
        startImageWriters()

        val source = directory.resolve("image.avif")
        Files.write(source, AVIF_IMAGE_BYTES)

        var processed = 0
        var failed = 0

        runBlocking {
            useCase.execute(
                basePath = source,
                fromFormat = ImageFromFormat.ALL,
                toFormat = Format.PNG,
                includeArchiveFiles = false,
                context = ProcessingContext(
                    setTotalFn = {},
                    updateCurrentFileFn = {},
                    processWithCountFn = { it() },
                    incrementCurrentFn = {},
                    incrementProcessedFn = { processed++ },
                    incrementFailedFn = { failed++ },
                    printErrorFn = { throw it },
                    yieldFn = {}
                )
            )
        }

        val converted = directory.resolve("image.png")
        assertFalse(source.exists())
        assertTrue(converted.exists())
        assertEquals(1, processed)
        assertEquals(0, failed)
        ImageIO.read(converted.toFile()).let { image ->
            assertEquals(2, image.width)
            assertEquals(2, image.height)
        }
    }

    @Test
    fun `exposes AVIF as a selectable target extension`() {
        assertTrue("avif" in useCase.supportedTargetExtensions())
        assertTrue(ImageFromFormat.AVIF.matchesExtension("AVIF"))
        assertFalse(ImageFromFormat.AVIF.matchesExtension("webp"))
        assertFalse(ImageFromFormat.AVIF.matches(Format.WEBP))
    }

    @Test
    fun `keeps both files when the converted file already exists`() = withTempDirectory { directory ->
        startImageWriters()
        val source = directory.resolve("photo.png")
        Files.write(source, pngBytes(4, 4))
        val existing = directory.resolve("photo.jpg")
        val existingBytes = jpegBytes(8, 8)
        Files.write(existing, existingBytes)
        val context = RecordingProcessingContext()

        runBlocking { useCase.execute(directory, ImageFromFormat.PNG, Format.JPEG, false, context.toProcessingContext()) }

        assertEquals(0, context.processed)
        assertEquals(1, context.failed)
        assertIs<FileAlreadyExistsException>(context.errors.single())
        assertTrue(source.exists())
        assertContentEquals(existingBytes, existing.readBytes())
    }

    @Test
    fun `replaces a misnamed image in place when only the extension case differs`() = withTempDirectory { directory ->
        startImageWriters()
        // JPEG data behind an upper-case PNG extension; on case-insensitive file systems the target is the same file.
        Files.write(directory.resolve("image.PNG"), jpegBytes(4, 4))
        val context = RecordingProcessingContext()

        runBlocking { useCase.execute(directory, ImageFromFormat.ALL, Format.PNG, false, context.toProcessingContext()) }

        assertEquals(1, context.processed)
        assertEquals(0, context.failed)
        val remaining = directory.listDirectoryEntries().single()
        assertTrue(remaining.name.equals("image.png", ignoreCase = true))
        assertEquals(Format.PNG, FormatDetector.detect(remaining.readBytes()).get())
    }

    @Test
    fun `fails an image too large for WebP without leaving files behind`() = withTempDirectory { directory ->
        startImageWriters()
        val source = directory.resolve("tall.png")
        val sourceBytes = pngBytes(1, OVERSIZED_DIMENSION)
        Files.write(source, sourceBytes)
        val context = RecordingProcessingContext()

        runBlocking { useCase.execute(directory, ImageFromFormat.ALL, Format.WEBP, false, context.toProcessingContext()) }

        assertEquals(0, context.processed)
        assertEquals(1, context.failed)
        assertTrue(context.errors.single().message.orEmpty().contains("16383"))
        assertEquals(listOf("tall.png"), directory.listDirectoryEntries().map { it.name })
        assertContentEquals(sourceBytes, source.readBytes())
    }

    @Test
    fun `finds images that WebP cannot encode in directories and archives`() = withTempDirectory { directory ->
        Files.write(directory.resolve("small.png"), pngBytes(2, 2))
        Files.write(directory.resolve("tall.png"), pngBytes(1, OVERSIZED_DIMENSION))
        Files.write(directory.resolve("wide.jpg"), jpegBytes(OVERSIZED_DIMENSION, 1))
        writeZip(
            directory.resolve("book.cbz"),
            "pages/001.png" to pngBytes(2, 2),
            "pages/002.png" to pngBytes(1, OVERSIZED_DIMENSION)
        )

        fun find(fromFormat: ImageFromFormat, toFormat: Format, includeArchiveFiles: Boolean): List<OversizedImage> =
            runBlocking {
                useCase.findOversizedImages(
                    directory,
                    fromFormat,
                    toFormat,
                    includeArchiveFiles,
                    RecordingProcessingContext().toProcessingContext()
                )
            }.sortedBy { it.path }

        assertEquals(
            listOf(
                OversizedImage("book.cbz/pages/002.png", ImageSize(1, OVERSIZED_DIMENSION)),
                OversizedImage("tall.png", ImageSize(1, OVERSIZED_DIMENSION)),
                OversizedImage("wide.jpg", ImageSize(OVERSIZED_DIMENSION, 1))
            ),
            find(ImageFromFormat.ALL, Format.WEBP, includeArchiveFiles = true)
        )
        assertEquals(
            listOf("tall.png"),
            find(ImageFromFormat.PNG, Format.WEBP, includeArchiveFiles = false).map { it.path }
        )
        assertEquals(emptyList<OversizedImage>(), find(ImageFromFormat.ALL, Format.PNG, includeArchiveFiles = true))
    }

    @Test
    fun `leaves archives without images to convert untouched`() = withTempDirectory { directory ->
        startImageWriters()
        val archive = directory.resolve("book.cbz")
        writeZip(archive, "001.jpg" to jpegBytes(2, 2), "notes.txt" to "notes".toByteArray())
        val archiveBytes = archive.readBytes()
        val lastModified = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"))
        Files.setLastModifiedTime(archive, lastModified)
        val context = RecordingProcessingContext()

        runBlocking { useCase.execute(directory, ImageFromFormat.ALL, Format.JPEG, true, context.toProcessingContext()) }

        assertEquals(0, context.processed)
        assertEquals(0, context.failed)
        assertContentEquals(archiveBytes, archive.readBytes())
        assertEquals(lastModified, Files.getLastModifiedTime(archive))
    }

    @Test
    fun `converts images inside archives and stores them uncompressed`() = withTempDirectory { directory ->
        startImageWriters()
        val archive = directory.resolve("book.cbz")
        writeZip(archive, "pages/001.png" to pngBytes(2, 2), "notes.txt" to "notes".toByteArray())
        val context = RecordingProcessingContext()

        runBlocking { useCase.execute(directory, ImageFromFormat.ALL, Format.JPEG, true, context.toProcessingContext()) }

        assertEquals(1, context.processed)
        assertEquals(0, context.failed)
        ZipFile(archive.toFile()).use { zipFile ->
            val entries = zipFile.entries().asSequence().associateBy { it.name }
            assertEquals(setOf("pages/001.jpg", "notes.txt"), entries.keys)
            assertEquals(ZipEntry.STORED, entries.getValue("pages/001.jpg").method)
            assertEquals(ZipEntry.DEFLATED, entries.getValue("notes.txt").method)
            val image = zipFile.getInputStream(entries.getValue("pages/001.jpg")).use { it.readBytes() }
            assertEquals(Format.JPEG, FormatDetector.detect(image).get())
        }
    }

    private fun startImageWriters() {
        startKoin {
            modules(
                module {
                    single { PngWriter() }
                    single { JpegWriter.Default }
                    single { GifWriter.Default }
                    single { WebpWriter.DEFAULT }
                }
            )
        }
    }

    private fun pngBytes(width: Int, height: Int): ByteArray =
        ImmutableImage.create(width, height).bytes(PngWriter.MaxCompression)

    private fun jpegBytes(width: Int, height: Int): ByteArray =
        ImmutableImage.create(width, height).bytes(JpegWriter.Default)

    private fun writeZip(path: Path, vararg entries: Pair<String, ByteArray>) {
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("junkyard-image-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private companion object {
        // One pixel past the WebP limit of 16383 x 16383.
        const val OVERSIZED_DIMENSION = 16_384

        val AVIF_IMAGE_BYTES: ByteArray = Base64.getDecoder().decode(
            "AAAAIGZ0eXBhdmlmAAAAAE1pUHJhdmlmbWlhZm1pZjEAAAG3bWV0YQAAAAAAAAAhaGRscgAAAAAAAAAAcGljdAAAAAAA" +
                "AAAAAAAAAAAAAAAkZGluZgAAABxkcmVmAAAAAAAAAAEAAAAMdXJsIAAAAAEAAAAOcGl0bQAAAAAAAQAAADhpaW5mAAAA" +
                "AAACAAAAFWluZmUCAAAAAAEAAGF2MDEAAAAAFWluZmUCAAABAAIAAGF2MDEAAAAAGmlyZWYAAAAAAAAADmF1eGwAAgAB" +
                "AAEAAADaaXBycAAAALFpcGNvAAAAE2NvbHJuY2x4AAIAAgAGgAAAAAxjbGxpAMsAQAAAABRpc3BlAAAAAAAAAAIAAAAC" +
                "AAAACWlyb3QAAAAAEHBpeGkAAAAAAwgICAAAAA5waXhpAAAAAAEIAAAAN2F1eEMAAAAAdXJuOm1wZWc6aGV2YzoyMDE1" +
                "OmF1eGlkOjEAAAAADAAAAAhOAaUEAAH+QAAAAAxhdjFDgQAMAAAAAAxhdjFDgQAcAAAAACFpcG1hAAAAAAAAAAIAAQaB" +
                "AgMFiIQAAgUDBoeJhAAAACxpbG9jAAAAAEQAAAIAAQAAAAEAAAHnAAAALAACAAAAAQAAAhMAAAAXAAAAAW1kYXQAAAAA" +
                "AAAAUxIACgwAAAAABn/8CBAQNCAyGhABkgAIIIIgTrIxaEDGvZ49Az8fD/q++aR9EgAKCAAAAAAGf/wVMgkQAY4AIIg" +
                "BSCA="
        )
    }
}
