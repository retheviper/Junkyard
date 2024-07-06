package framework

import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.WebpWriter
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

fun Format.toExtension() = when (this) {
    Format.JPEG -> listOf("jpg", "jpeg")
    Format.PNG -> listOf("png")
    Format.WEBP -> listOf("webp")
    Format.GIF -> listOf("gif")
}

fun KoinComponent.getSuitableImageWriter(format: Format) = when (format) {
    Format.JPEG -> get<JpegWriter>()
    Format.PNG -> get<PngWriter>()
    Format.GIF -> get<GifWriter>()
    Format.WEBP -> get<WebpWriter>()
}