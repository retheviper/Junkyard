package application.model

import infrastructure.system.OS

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
