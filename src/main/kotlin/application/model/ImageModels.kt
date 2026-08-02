package application.model

import com.sksamuel.scrimage.format.Format

enum class ArchiveFormat {
    ZIP, CBZ
}

enum class ImageFromFormat(
    val format: Format?,
    val extensions: Set<String>
) {
    ALL(null, emptySet()),
    JPEG(Format.JPEG, setOf("jpg", "jpeg")),
    PNG(Format.PNG, setOf("png")),
    GIF(Format.GIF, setOf("gif")),
    WEBP(Format.WEBP, setOf("webp")),
    AVIF(null, setOf("avif"));

    fun matches(target: Format): Boolean = this == ALL || format == target

    fun matchesExtension(extension: String): Boolean =
        this == ALL || extensions.any { it.equals(extension, ignoreCase = true) }
}
