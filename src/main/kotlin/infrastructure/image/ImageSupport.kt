package infrastructure.image

import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.WebpWriter
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// libwebp (cwebp, gif2webp) rejects pictures wider or taller than this with BAD_DIMENSION.
private const val WEBP_MAX_DIMENSION = 16_383

data class ImageSize(val width: Int, val height: Int)

fun Format.toExtension() = when (this) {
    Format.JPEG -> listOf("jpg", "jpeg")
    Format.PNG -> listOf("png")
    Format.WEBP -> listOf("webp")
    Format.GIF -> listOf("gif")
}

/** The largest width and height this format can encode, or null when the limit is not a practical concern. */
fun Format.maxDimension(): Int? = when (this) {
    Format.WEBP -> WEBP_MAX_DIMENSION
    Format.JPEG, Format.PNG, Format.GIF -> null
}

fun Format.canEncode(width: Int, height: Int): Boolean {
    val maxDimension = maxDimension() ?: return true
    return width <= maxDimension && height <= maxDimension
}

fun KoinComponent.getSuitableImageWriter(format: Format) = when (format) {
    Format.JPEG -> get<JpegWriter>()
    Format.PNG -> get<PngWriter>()
    Format.GIF -> get<GifWriter>()
    Format.WEBP -> get<WebpWriter>()
}

/**
 * Reads the size of the first image from its header without decoding pixels.
 * Returns null when no installed ImageIO reader understands the data. [input] is left open.
 */
fun readImageSize(input: InputStream): ImageSize? =
    MemoryCacheImageInputStream(input).use { imageInput ->
        val reader = ImageIO.getImageReaders(imageInput).asSequence().firstOrNull() ?: return null
        try {
            reader.setInput(imageInput, true, true)
            ImageSize(reader.getWidth(0), reader.getHeight(0))
        } finally {
            reader.dispose()
        }
    }
