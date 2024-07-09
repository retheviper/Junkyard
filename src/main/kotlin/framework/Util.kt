package framework

enum class OS {
    WINDOWS,
    MAC,
    LINUX,
    OTHER;

    companion object {
        val current: OS
            get() = getOS()
    }
}

fun getOS(): OS {
    val os = System.getProperty("os.name").lowercase()

    return when {
        os.contains("win") -> OS.WINDOWS
        os.contains("mac") -> OS.MAC
        os.contains("nix") || os.contains("nux") || os.contains("aix") -> OS.LINUX
        else -> OS.OTHER
    }
}