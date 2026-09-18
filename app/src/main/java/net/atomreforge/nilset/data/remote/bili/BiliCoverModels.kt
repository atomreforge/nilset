package net.atomreforge.nilset.data.remote.bili

import kotlinx.serialization.Serializable
import net.atomreforge.nilset.core.bili.BiliContentKind
import java.io.File

@Serializable
data class BiliVideoOwner(
    val mid: Long? = null,
    val name: String? = null,
)

@Serializable
data class BiliVideoData(
    val bvid: String? = null,
    val aid: Long? = null,
    val pic: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val owner: BiliVideoOwner? = null,
)

@Serializable
data class BiliVideoEnvelope(
    override val code: Int = Int.MIN_VALUE,
    override val message: String? = null,
    val data: BiliVideoData? = null,
) : BiliEnvelope

@Serializable
data class BiliArticleData(
    val banner_url: String? = null,
    val image_urls: List<String>? = null,
    val title: String? = null,
    val author_name: String? = null,
    val author: BiliVideoOwner? = null,
)

@Serializable
data class BiliArticleEnvelope(
    override val code: Int = Int.MIN_VALUE,
    override val message: String? = null,
    val data: BiliArticleData? = null,
) : BiliEnvelope

@Serializable
data class BiliLiveData(
    val user_cover: String? = null,
    val title: String? = null,
    val uid: Long? = null,
)

@Serializable
data class BiliLiveEnvelope(
    override val code: Int = Int.MIN_VALUE,
    override val message: String? = null,
    val data: BiliLiveData? = null,
) : BiliEnvelope

@Serializable
data class BiliDynamicAuthorModule(
    val name: String? = null,
    val mid: Long? = null,
)

@Serializable
data class BiliDynamicDrawItem(
    val src: String? = null,
)

@Serializable
data class BiliDynamicMajorArchive(
    val bvid: String? = null,
    val pic: String? = null,
)

@Serializable
data class BiliDynamicMajor(
    val draw: BiliDynamicDrawWrapper? = null,
    val archive: BiliDynamicMajorArchive? = null,
)

@Serializable
data class BiliDynamicDrawWrapper(
    val items: List<BiliDynamicDrawItem>? = null,
)

@Serializable
data class BiliDynamicDesc(
    val text: String? = null,
)

@Serializable
data class BiliDynamicModuleDynamic(
    val desc: BiliDynamicDesc? = null,
    val major: BiliDynamicMajor? = null,
)

@Serializable
data class BiliDynamicModules(
    @kotlinx.serialization.SerialName("module_author") val moduleAuthor: BiliDynamicAuthorModule? = null,
    @kotlinx.serialization.SerialName("module_dynamic") val moduleDynamic: BiliDynamicModuleDynamic? = null,
)

@Serializable
data class BiliDynamicItem(
    @kotlinx.serialization.SerialName("id_str") val idStr: String? = null,
    val modules: BiliDynamicModules? = null,
)

@Serializable
data class BiliDynamicData(
    val item: BiliDynamicItem? = null,
)

@Serializable
data class BiliDynamicEnvelope(
    override val code: Int = Int.MIN_VALUE,
    override val message: String? = null,
    val data: BiliDynamicData? = null,
) : BiliEnvelope

interface BiliEnvelope {
    val code: Int
    val message: String?
}

data class BiliCoverDetails(
    val input: BiliInputReference,
    val title: String,
    val imageUrl: String,
    val author: String? = null,
    val uid: Long? = null,
    val description: String? = null,
)

data class BiliInputReference(
    val kind: BiliContentKind,
    val id: String,
) {
    val displayName: String
        get() = when (kind) {
            BiliContentKind.AV -> "av$id"
            BiliContentKind.BV -> "BV$id"
            BiliContentKind.CV -> "cv$id"
            BiliContentKind.LIVE -> "live$id"
            BiliContentKind.DYNAMIC -> "dynamic$id"
        }
}

data class BiliCoverFile(
    val file: File,
    val mimeType: String,
    val extension: String,
)

class BiliCoverException(
    val code: Int,
    message: String,
) : IllegalStateException(message)
