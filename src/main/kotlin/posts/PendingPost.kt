package posts

import kotlinx.serialization.Serializable

@Serializable
data class PendingPost(
    val id: String,
    val imagePath: String,
    val imageExt: String,
    val pendingBlocks: List<PendingBlock>? = null,
)

@Serializable
data class PendingBlock(
    val id: String,
    val imagePath: String,
)
