package application.usecase

import application.model.VideoCodec
import application.model.VideoFormat
import application.processing.ProcessingContext
import infrastructure.binary.BinaryBundleService
import infrastructure.system.OS
import java.nio.file.FileAlreadyExistsException
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
        // Never overwrite: "a.avi" and "a.mov" both map to "a.mp4", and each original is deleted after conversion.
        if (Files.exists(targetPath)) {
            throw FileAlreadyExistsException(filePath.toString(), targetPath.toString(), "converted file already exists")
        }

        val process = ProcessBuilder(
            ffmpegPath,
            "-hide_banner",
            "-nostats",
            "-nostdin",
            "-n",
            "-i", filePath.absolutePathString(),
            "-c:v", encoder,
            "-c:a", "aac",
            targetPath.absolutePathString()
        )
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()

        // Closing the app must not leave ffmpeg running in the background or a truncated output behind.
        val shutdownHook = thread(start = false, name = "ffmpeg-shutdown") {
            if (process.isAlive) {
                process.destroyForcibly().waitFor(2, TimeUnit.SECONDS)
                Files.deleteIfExists(targetPath)
            }
        }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        // FFmpeg reports the actual failure at the end of its output, so only the last lines are kept.
        val errorTail = ArrayDeque<String>()
        val errorReader = thread(
            start = true,
            isDaemon = true,
            name = "ffmpeg-error-reader"
        ) {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    errorTail.addLast(line)
                    if (errorTail.size > MAX_FFMPEG_ERROR_LINES) {
                        errorTail.removeFirst()
                    }
                }
            }
        }

        try {
            val exitCode = runInterruptible { process.waitFor() }
            errorReader.join()

            if (exitCode != 0) {
                throw RuntimeException("FFmpeg failed with exit code $exitCode. Error:\n${errorTail.joinToString("\n")}")
            }
        } catch (error: Throwable) {
            process.destroy()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
            Files.deleteIfExists(targetPath)
            throw error
        } finally {
            // Removing the hook fails once the JVM is already shutting down, when the hook itself handles cleanup.
            runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
        }
    }

    private companion object {
        const val MAX_FFMPEG_ERROR_LINES = 50
    }
}
