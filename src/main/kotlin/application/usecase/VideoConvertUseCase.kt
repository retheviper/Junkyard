package application.usecase

import application.model.VideoCodec
import application.model.VideoFormat
import application.processing.ProcessingContext
import infrastructure.binary.BinaryBundleService
import infrastructure.system.OS
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class VideoConvertUseCase(
    private val binaryBundleService: BinaryBundleService
) {
    private val ffmpegPath: String by lazy {
        val ffmpegDir = when (OS.current) {
            OS.WINDOWS -> "windows"
            OS.MAC -> "macos"
            OS.LINUX -> "linux"
            OS.OTHER -> throw IllegalStateException("Unsupported OS")
        }

        val executableName = if (OS.current == OS.WINDOWS) "ffmpeg.exe" else "ffmpeg"
        binaryBundleService.getBinaryBundle("/binaries/ffmpeg/$ffmpegDir/$executableName").absolutePathString()
    }

    suspend fun execute(
        basePath: Path,
        targetFormat: VideoFormat,
        videoCodec: VideoCodec,
        useHardwareEncoder: Boolean,
        context: ProcessingContext
    ) {
        val targets = Files.walk(basePath).use { stream ->
            stream.filter { file -> Files.isRegularFile(file) }
                .filter { file ->
                    targetFormat.extensions.any { it.equals(file.extension, ignoreCase = true) }
                }
                .toList()
        }

        context.setTotal(targets.size)

        val encoder = videoCodec.getEncoder(useHardwareEncoder)

        targets.forEach { file ->
            context.checkpoint()
            context.updateCurrentFile(file)
            runCatching { convertVideo(encoder, file) }
                .onSuccess {
                    Files.deleteIfExists(file)
                    context.incrementProcessed()
                }
                .onFailure {
                    if (it is CancellationException) {
                        throw it
                    }
                    context.incrementFailed()
                    context.printError(it)
                }
            context.incrementCurrent()
        }
    }

    private suspend fun convertVideo(encoder: String, filePath: Path) = withContext(Dispatchers.IO) {
        val targetPath = filePath.resolveSibling("${filePath.nameWithoutExtension}.mp4")

        val process = ProcessBuilder(
            ffmpegPath,
            "-y",
            "-i", filePath.absolutePathString(),
            "-c:v", encoder,
            "-c:a", "aac",
            targetPath.absolutePathString()
        )
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

        val errorOutput = StringBuilder()
        val errorReader = thread(
            start = true,
            isDaemon = true,
            name = "ffmpeg-error-reader"
        ) {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (errorOutput.length < MAX_FFMPEG_ERROR_LENGTH) {
                        errorOutput.appendLine(line)
                    }
                }
            }
        }

        try {
            val exitCode = runInterruptible { process.waitFor() }
            errorReader.join()

            if (exitCode != 0) {
                throw RuntimeException("FFmpeg failed with exit code $exitCode. Error: $errorOutput")
            }
        } catch (error: CancellationException) {
            process.destroy()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
            throw error
        }
    }

    private companion object {
        const val MAX_FFMPEG_ERROR_LENGTH = 16_384
    }
}
