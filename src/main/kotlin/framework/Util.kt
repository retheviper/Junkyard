package framework

enum class OS {
    WINDOWS,
    MAC,
    LINUX,
    OTHER;

    companion object {
        private val os = System.getProperty("os.name").lowercase()

        val current: OS
            get() = when {
                os.contains("win") -> WINDOWS
                os.contains("mac") -> MAC
                os.contains("nix") || os.contains("nux") || os.contains("aix") -> LINUX
                else -> OTHER
            }
    }
}