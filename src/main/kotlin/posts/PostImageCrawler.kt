package posts

import Constant
import extension.getEnv
import kotlinx.serialization.json.Json
import utils.HttpRequestUtil
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
            println("${index + 1}/${pendingPosts.size} PROCESSING POST: ${post.id}")
            var allSuccess = true

            val imageUrl = "$postImageDomain${post.imagePath}"
            val success = crawlAndOptimizeImage(imageUrl, post.id, post.imageExt, postImagesPath)
            if (!success) allSuccess = false

            val postBlocksDir = File(postImagesPath, post.id).path

            post.pendingBlocks?.forEach { block ->
                System.out.println("${index + 1}/${block.id}")
                val blockExt = File(block.imagePath).extension.let { if (it.isNotEmpty()) ".$it" else "" }
                val blockImageUrl = "$postImageDomain${block.imagePath}"
                val blockSuccess = crawlAndOptimizeImage(blockImageUrl, block.id, blockExt, postBlocksDir)
                if (!blockSuccess) allSuccess = false
            }

            if (allSuccess) {
                successPosts.add(post)
            }
        }

        if (successPosts.isNotEmpty()) {
            publishPostImages(successPosts.map { it.id })
        }

        println("Crawled success posts: ${successPosts.map { it.id }}")
    }

    private fun crawlAndOptimizeImage(imageUrl: String, imageId: String, imageExt: String, outputDir: String): Boolean {
        File(outputDir).mkdirs()
        val rawOutputPath = File(outputDir, "${imageId}raw${imageExt}").path
        val outputPath = File(outputDir, "${imageId}${imageExt}").path
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
            val rawLog = if (enableRawImage) "raw=$rawOutputPath, " else ""
            println("  -> CRAW SUCCESS: ${imageId} -> ${rawLog}optimized=$outputPath, optimizedSuccess=$optimized")
            return true
        } else {
            println("  -> CRAW FAIL: ${imageId} ($imageUrl)")
            return false
        }
    }

    fun publishPostImages(postIds: List<String>) {
        val secretKey = "${getEnv(Constant.ENV_SECRET_KEY)}"
        if (secretKey.isBlank()) {
            println("SECRET_KEY is missing")
            return
        }

        if (postIds.isEmpty()) {
            println("No post IDs provided")
            return
        }

        val baseUrl = getEnv(Constant.ENV_ADMIN_URL).orEmpty()
        if (baseUrl.isBlank()) {
            println("ADMIN_URL is missing")
            return
        }

        val endpoint = "$baseUrl/api/fastscore/posts/publish"

        val bodyMap = mapOf("post_ids" to postIds)
        val (success, error) = HttpRequestUtil.doPost(endpoint, bodyMap, mapOf("Authorization" to "Bearer $secretKey"))
        if (success) {
            println("PUBLISH SUCCESS: $postIds")
        } else {
//            println(endpoint)
//            println("PUBLISH FAIL: $postIds | error=$error")
        }
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
