package application.usecase

import application.model.VideoCodec
import application.model.VideoFormat
import application.processing.ProcessingContext
import infrastructure.binary.BinaryBundleService
import infrastructure.system.OS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

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

        binaryBundleService.getBinaryBundle("/binaries/ffmpeg/$ffmpegDir/ffmpeg").absolutePathString()
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
            context.processWithCount {
                context.updateCurrentFile(file)
                runCatching { convertVideo(encoder, file) }
                    .onSuccess { Files.deleteIfExists(file) }
                    .getOrThrow()
            }
        }
    }

    private fun convertVideo(encoder: String, filePath: Path) {
        val targetPath = filePath.resolveSibling("${filePath.nameWithoutExtension}.mp4")

        val process = ProcessBuilder(
            ffmpegPath,
            "-y",
            "-i", filePath.absolutePathString(),
            "-c:v", encoder,
            "-c:a", "aac",
            targetPath.absolutePathString()
        ).apply {
            environment()["PATH"] = System.getenv("PATH")
            environment()["DYLD_LIBRARY_PATH"] = System.getenv("DYLD_LIBRARY_PATH")
        }.start()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val errorStream = process.errorStream.bufferedReader().use { it.readText() }
            throw RuntimeException("FFmpeg failed with exit code $exitCode. Error: $errorStream")
        }
    }
}
