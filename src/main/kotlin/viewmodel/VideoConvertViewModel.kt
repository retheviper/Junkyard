package viewmodel

import framework.OS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import service.BinaryBundleService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

enum class VideoFormat(val extensions: List<String>) {
    ALL(listOf("avi", "m4v", "ts", "webm", "wmv", "mov", "mpg", "mpeg")),
    AVI(listOf("avi")),
    M4V(listOf("m4v")),
    TS(listOf("ts")),
    WEBM(listOf("webm")),
    WMV(listOf("wmv")),
    MOV(listOf("mov")),
    MPG(listOf("mpg", "mpeg"))
}

enum class VideoCodec(
    val softwareEncoder: String,
    val hardwareEncoders: Map<OS, String>
) {
    H264(
        softwareEncoder = "libx264",
        hardwareEncoders = mapOf(
            OS.WINDOWS to "h264_nvenc",
            OS.MAC to "h264_videotoolbox",
            OS.LINUX to "h264_nvenc"
        )
    ),
    H265(
        softwareEncoder = "libx265",
        hardwareEncoders = mapOf(
            OS.WINDOWS to "hevc_nvenc",
            OS.MAC to "hevc_videotoolbox",
            OS.LINUX to "hevc_nvenc"
        )
    );

    fun getEncoder(useHardware: Boolean): String {
        return if (useHardware) hardwareEncoders[OS.current] ?: softwareEncoder else softwareEncoder
    }
}

class VideoConvertViewModel : ProcessViewModel(), KoinComponent {
    private val binaryBundleService: BinaryBundleService by inject()
    override val targetPickerType: TargetPickerType = TargetPickerType.DIRECTORY

    private val _targetFormat = MutableStateFlow(VideoFormat.ALL)
    val targetFormat = _targetFormat.asStateFlow()

    private val _videoCodec = MutableStateFlow(VideoCodec.H264)
    val videoCodec = _videoCodec.asStateFlow()

    private val _useHardwareEncoder = MutableStateFlow(true)
    val useHardwareEncoder = _useHardwareEncoder.asStateFlow()

    fun setTargetFormat(format: VideoFormat) {
        _targetFormat.value = format
    }

    fun setVideoCodec(codec: VideoCodec) {
        _videoCodec.value = codec
    }

    fun toggleUseHardwareEncoder() {
        _useHardwareEncoder.value = !_useHardwareEncoder.value
    }

    private val ffmpegPath: String by lazy {
        val ffmpegDir = when (OS.current) {
            OS.WINDOWS -> "windows"
            OS.MAC -> "macos"
            OS.LINUX -> "linux"
            OS.OTHER -> throw IllegalStateException("Unsupported OS")
        }

        binaryBundleService.getBinaryBundle("/binaries/ffmpeg/$ffmpegDir/ffmpeg").absolutePathString()
    }

    override fun onProcessClick() {
        process { basePath ->
            val targets = Files.walk(basePath).use { stream ->
                stream.filter { file -> Files.isRegularFile(file) }
                    .filter { file ->
                        targetFormat.value.extensions.any { it.equals(file.extension, ignoreCase = true) }
                    }
                    .toList()
            }

            setTotal(targets.size)

            val encoder = videoCodec.value.getEncoder(useHardwareEncoder.value)

            targets.forEach { file ->
                yield()
                processWithCount {
                    updateCurrentFile(file)
                    runCatching { convertVideo(encoder, file) }
                        .onSuccess { Files.deleteIfExists(file) }
                        .getOrThrow()
                }
            }
        }
    }

    private fun convertVideo(encoder: String, filePath: Path) {
        val targetPath =
            filePath.resolveSibling("${filePath.nameWithoutExtension}.mp4")

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