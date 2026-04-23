package posts

import Constant
import extension.getEnv
import kotlinx.serialization.json.Json
import utils.ImageCrawlerUtil
import java.io.File

object PostImageCrawler {

    private const val pendingPostsJsonPath = "assets/config/pending-posts.json"
    private const val postImagesPath = "assets/image/posts"
    private const val postImageMaxWidth = 1020
    private const val enableRawImage = false
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchPostImages() {
        val postImageDomain = getEnv(Constant.ENV_POST_IMAGES_DOMAIN).orEmpty()
        if (postImageDomain.isBlank()) {
            println("POST_IMAGE_DOMAIN is missing")
            return
        }

        val pendingPosts = loadPendingPosts()
        if (pendingPosts.isEmpty()) {
            println("No pending post found")
            return
        }

        val successPosts = mutableListOf<PendingPost>()
        pendingPosts.forEachIndexed { index, post ->
            val imageUrl = "$postImageDomain${post.imagePath}"
            val rawOutputPath = File(postImagesPath, "${post.id}raw${post.imageExt}").path
            val outputPath = File(postImagesPath, "${post.id}${post.imageExt}").path
            val crawlOutputPath = if (enableRawImage) rawOutputPath else outputPath
            val success = ImageCrawlerUtil.crawlImage(imageUrl, crawlOutputPath)
            if (success) {
                if (enableRawImage) {
                    File(rawOutputPath).copyTo(File(outputPath), overwrite = true)
                }
                val optimized = ImageCrawlerUtil.resizeAndCompressIfNeeded(
                    outputPath = outputPath,
                    maxWidth = postImageMaxWidth
                )
                successPosts.add(post)
                val rawLog = if (enableRawImage) "raw=$rawOutputPath, " else ""
                println("${index + 1}/${pendingPosts.size} CRAW SUCCESS: ${post.id} -> ${rawLog}optimized=$outputPath, optimizedSuccess=$optimized")
            } else {
                println("${index + 1}/${pendingPosts.size} CRAW FAIL: ${post.id} ($imageUrl)")
            }
        }

        println("Crawled success posts: ${successPosts.map { it.id }}")
    }

    private fun loadPendingPosts(): List<PendingPost> {
        val pendingPostFile = File(pendingPostsJsonPath)
        if (!pendingPostFile.exists()) {
            println("Pending posts file not found: $pendingPostsJsonPath")
            return emptyList()
        }
        return json.decodeFromString<List<PendingPost>>(pendingPostFile.readText())
    }
}
