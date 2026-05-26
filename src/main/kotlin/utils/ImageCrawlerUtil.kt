package utils

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import java.io.ByteArrayOutputStream

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

    /**
     * Optimizes the image at the given file path to be under the specified max size in bytes.
     * Shrinks/resizes the image dimensions iteratively until it fits the limit.
     */
    fun optimizeImageSize(file: File, maxSizeBytes: Long = 20480L): Boolean {
        if (!file.exists() || !file.isFile) return false
        val currentSize = file.length()
        if (currentSize <= maxSizeBytes) return false

        try {
            val extension = file.extension.lowercase()
            if (extension != "png" && extension != "jpg" && extension != "jpeg" && extension != "gif") {
                return false
            }

            var image = ImageIO.read(file) ?: return false
            val format = if (extension == "jpg" || extension == "jpeg") "jpg" else "png"
            
            // Estimate starting scale based on quadratic area ratio to minimize iterations
            val sizeRatio = maxSizeBytes.toDouble() / currentSize
            var scale = Math.sqrt(sizeRatio) * 1.2
            if (scale >= 1.0) {
                scale = 0.9
            } else if (scale < 0.1) {
                scale = 0.1
            }

            var width = image.width
            var height = image.height
            var finalBytes: ByteArray? = null
            var attempts = 0

            while (attempts < 15) {
                val bos = ByteArrayOutputStream()
                ImageIO.write(image, format, bos)
                val bytes = bos.toByteArray()
                
                if (bytes.size <= maxSizeBytes) {
                    finalBytes = bytes
                    break
                }
                
                finalBytes = bytes // Keep the smallest so far as fallback

                val newWidth = (image.width * scale).toInt().coerceAtLeast(16)
                val newHeight = (image.height * scale).toInt().coerceAtLeast(16)

                if (newWidth == width && newHeight == height) {
                    break // Cannot resize any smaller
                }
                width = newWidth
                height = newHeight

                // Preserve alpha/transparency channel if source has it
                val imageType = if (image.colorModel.hasAlpha()) {
                    BufferedImage.TYPE_INT_ARGB
                } else {
                    BufferedImage.TYPE_INT_RGB
                }

                val resized = BufferedImage(width, height, imageType)
                val g2d = resized.createGraphics()
                g2d.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
                g2d.drawImage(image, 0, 0, width, height, null)
                g2d.dispose()
                image = resized

                // Reduce scale for next iteration if still too large
                scale *= 0.8
                attempts++
            }

            if (finalBytes != null && finalBytes.size < currentSize) {
                Files.write(file.toPath(), finalBytes)
                println("Optimized: ${file.name} (${currentSize / 1024} KB -> ${finalBytes.size / 1024} KB, new size: ${image.width}x${image.height})")
                return true
            }
        } catch (e: Exception) {
            println("Failed to optimize image: ${file.absolutePath} due to ${e.message}")
            e.printStackTrace()
            return false
        }
        return false
    }
}

