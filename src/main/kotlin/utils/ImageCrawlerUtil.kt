package utils

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

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
}
