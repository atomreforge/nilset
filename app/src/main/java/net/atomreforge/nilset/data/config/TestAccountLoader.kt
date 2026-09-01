package net.atomreforge.nilset.data.config

import android.content.Context
import com.charleskorn.kaml.Yaml
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import net.atomreforge.nilset.const.ConfigFiles

@Serializable
data class TestAccount(
    val username: String,
    val nickname: String,
    val password: String,
    val useLocalLogin: Boolean = false,
)

@Singleton
class TestAccountLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun load(): TestAccount? {
        val exists = ConfigFiles.TEST_ACCOUNT in (context.assets.list("").orEmpty()).toSet()
        if (!exists) return null

        val text = context.assets.open(ConfigFiles.TEST_ACCOUNT).bufferedReader().use { it.readText() }
        return try {
            Yaml.default.decodeFromString(TestAccount.serializer(), text)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse ${ConfigFiles.TEST_ACCOUNT}: ${e.message}", e)
        }
    }
}
