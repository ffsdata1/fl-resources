package utils

import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

object ImageCrawlerUtil {

    fun crawlImage(imageUrl: String, outputPath: String): Boolean {
        val destinationFile = File(outputPath)
        try {
            destinationFile.parentFile?.mkdirs()
            val url = URL(imageUrl)
            val inputStream = url.openStream()
            Files.copy(inputStream, Paths.get(outputPath), StandardCopyOption.REPLACE_EXISTING)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun resizeAndCompressIfNeeded(outputPath: String, maxWidth: Int, quality: Float = 0.82f): Boolean {
        return try {
            val file = File(outputPath)
            if (!file.exists() || maxWidth <= 0) return false

            val original = ImageIO.read(file) ?: return false
            val targetImage = if (original.width > maxWidth) {
                val newHeight = (original.height.toDouble() * maxWidth / original.width).toInt().coerceAtLeast(1)
                val resized = BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB)
                val graphics = resized.createGraphics()
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val scaled: Image = original.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH)
                graphics.drawImage(scaled, 0, 0, null)
                graphics.dispose()
                resized
            } else {
                original
            }

            val format = file.extension.lowercase().ifBlank { "jpg" }
            if (format == "jpg" || format == "jpeg") {
                val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull() ?: return false
                file.outputStream().use { output ->
                    val outputStream = ImageIO.createImageOutputStream(output)
                    writer.output = outputStream
                    val writeParams = writer.defaultWriteParam
                    if (writeParams.canWriteCompressed()) {
                        writeParams.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        writeParams.compressionQuality = quality.coerceIn(0.1f, 1.0f)
                    }
                    writer.write(null, IIOImage(targetImage, null, null), writeParams)
                    outputStream.close()
                    writer.dispose()
                }
            } else {
                ImageIO.write(targetImage, format, file)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
