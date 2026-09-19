package net.atomreforge.nilset.data.repository

import net.atomreforge.nilset.core.bili.BiliInput
import net.atomreforge.nilset.core.bili.BiliInputParser
import net.atomreforge.nilset.data.downloads.BiliCoverMediaSaver
import net.atomreforge.nilset.data.remote.bili.BiliCoverDetails
import net.atomreforge.nilset.data.remote.bili.BiliCoverException
import net.atomreforge.nilset.data.remote.bili.BiliCoverFile
import net.atomreforge.nilset.data.remote.bili.BiliCoverRemoteDataSource
import net.atomreforge.nilset.data.remote.bili.BiliInputReference
import net.atomreforge.nilset.di.BiliCacheDirectory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliNilRepository @Inject constructor(
    private val remoteDataSource: BiliCoverRemoteDataSource,
    private val mediaSaver: BiliCoverMediaSaver,
    @BiliCacheDirectory private val cacheDirectory: File,
) {

    suspend fun resolve(rawInput: String): BiliCoverDetails {
        val shortLinkCode = BiliInputParser.parseShortLinkCode(rawInput)
        val input = (shortLinkCode
            ?.let { code ->
                remoteDataSource.resolveShortLink(code).let { reference ->
                    BiliInput(reference.kind, reference.id)
                }
            }
            ?: BiliInputParser.parseContent(rawInput)
                ?: throw BiliCoverException(0, "Invalid Bilibili input"))
        return remoteDataSource.resolve(BiliInputReference(input.kind, input.id))
    }

    suspend fun downloadCover(details: BiliCoverDetails): BiliCoverFile =
        remoteDataSource.downloadCover(details.imageUrl, details.input.displayName)

    suspend fun downloadCoverByUrl(rawUrl: String, displayName: String): BiliCoverFile =
        remoteDataSource.downloadCover(rawUrl, displayName)

    suspend fun saveCover(
        details: BiliCoverDetails,
        coverFile: BiliCoverFile,
    ): Result<String> = mediaSaver.save(
        source = coverFile,
        input = BiliInput(details.input.kind, details.input.id),
    )

    fun cacheDirectory(): File = cacheDirectory
}
