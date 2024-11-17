package viewmodel

import framework.OS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

enum class VideoFormat(val extensions: List<String>) {
    ALL(listOf("mp4", "avi", "m4v", "ts", "webm", "wmv", "mov", "mpg", "mpeg")),
    AVI(listOf("avi")),
    M4V(listOf("m4v")),
    TS(listOf("ts")),
    WEBM(listOf("webm")),
    WMV(listOf("wmv")),
    MOV(listOf("mov")),
    MPG(listOf("mpg", "mpeg"))
}

enum class VideoCodec {
    H264, H265
}

class VideoConvertViewModel : ProcessViewModel(), KoinComponent {
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

            targets.forEach { file ->
                yield()
                processWithCount {
                    updateCurrentFile(file)
                    runCatching { convertVideo(file) }
                        .onSuccess { Files.deleteIfExists(file) }
                        .getOrThrow()
                }
            }
        }
    }

    private fun convertVideo(filePath: Path) {
        val codec = getCodec(videoCodec.value)
        val targetPath =
            filePath.resolveSibling("${filePath.nameWithoutExtension}.mp4")

        val process = ProcessBuilder(
            "ffmpeg",
            "-i", filePath.absolutePathString(),
            "-c:v", codec,
            "-c:a", "aac",
            targetPath.absolutePathString()
        ).start()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val errorStream = process.errorStream.bufferedReader().use { it.readText() }
            throw RuntimeException("FFmpeg failed with exit code $exitCode. Error: $errorStream")
        }
    }

    private fun getCodec(codec: VideoCodec): String {
        val os = OS.current
        val useHardwareEncoder = useHardwareEncoder.value

        return when (codec) {
            VideoCodec.H264 -> {
                when (os) {
                    OS.WINDOWS -> if (useHardwareEncoder) "h264_nvenc" else "libx264"
                    OS.MAC -> if (useHardwareEncoder) "h264_videotoolbox" else "libx264"
                    OS.LINUX -> if (useHardwareEncoder) "h264_nvenc" else "libx264"
                    else -> "libx264"
                }
            }

            VideoCodec.H265 -> {
                when (os) {
                    OS.WINDOWS -> if (useHardwareEncoder) "hevc_nvenc" else "libx265"
                    OS.MAC -> if (useHardwareEncoder) "hevc_videotoolbox" else "libx265"
                    OS.LINUX -> if (useHardwareEncoder) "hevc_nvenc" else "libx265"
                    else -> "libx265"
                }
            }
        }
    }
}