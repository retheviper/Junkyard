package application.model

import com.sksamuel.scrimage.format.Format

enum class ArchiveFormat {
    ZIP, CBZ
}

enum class ImageFromFormat(val format: Format?) {
    ALL(null),
    JPEG(Format.JPEG),
    PNG(Format.PNG),
    GIF(Format.GIF),
    WEBP(Format.WEBP);

    fun matches(target: Format): Boolean = format == null || format == target
}
