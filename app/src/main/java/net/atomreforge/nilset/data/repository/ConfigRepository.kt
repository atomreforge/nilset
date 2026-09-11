package net.atomreforge.nilset.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.atomreforge.nilset.const.ConfigOptions
import net.atomreforge.nilset.const.ConfigStoreKeys
import net.atomreforge.nilset.di.ConfigDataStore
import net.atomreforge.nilset.di.ApplicationScope
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class ConfigEntry(
    val option: String,
    val value: String,
    val defaultValue: String,
)

interface ConfigRepository {
    val baseUrl: StateFlow<String>

    fun get(option: String): Result<ConfigEntry>

    fun set(option: String, value: String): Result<Unit>

    fun clear(option: String): Result<Unit>

    fun list(): List<ConfigEntry>
}

@Singleton
class PreferencesConfigRepository @Inject constructor(
    @param:ConfigDataStore private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
) : ConfigRepository {
    private val _hostAddr = MutableStateFlow(ConfigOptions.DEFAULT_HOST_ADDR)
    override val baseUrl: StateFlow<String> = _hostAddr
        .map(HostAddressNormalizer::toBaseUrl)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = HostAddressNormalizer.toBaseUrl(ConfigOptions.DEFAULT_HOST_ADDR),
        )

    init {
        scope.launch {
            val storedValue = dataStore.data.first()[stringPreferencesKey(ConfigStoreKeys.HOST_ADDR)]
            if (storedValue != null) {
                HostAddressNormalizer.normalize(storedValue).onSuccess { value ->
                    _hostAddr.value = value
                }
            }
        }
    }

    override fun get(option: String): Result<ConfigEntry> = entry(option)

    override fun set(option: String, value: String): Result<Unit> = runCatching {
        requireSupportedOption(option)
        val normalizedValue = HostAddressNormalizer.normalize(value).getOrThrow()
        _hostAddr.value = normalizedValue
        persistHostIp(normalizedValue)
    }

    override fun clear(option: String): Result<Unit> = runCatching {
        requireSupportedOption(option)
        _hostAddr.value = ConfigOptions.DEFAULT_HOST_ADDR
        clearPersistedHostIp()
    }

    override fun list(): List<ConfigEntry> = listOf(entry(ConfigOptions.HOST_ADDR).getOrThrow())

    private fun entry(option: String): Result<ConfigEntry> = runCatching {
        requireSupportedOption(option)
        ConfigEntry(
            option = option,
            value = _hostAddr.value,
            defaultValue = ConfigOptions.DEFAULT_HOST_ADDR,
        )
    }

    private fun requireSupportedOption(option: String) {
        require(option == ConfigOptions.HOST_ADDR) { "未知配置项：$option" }
    }

    private fun persistHostIp(value: String) {
        scope.launch {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(ConfigStoreKeys.HOST_ADDR)] = value
            }
        }
    }

    private fun clearPersistedHostIp() {
        scope.launch {
            dataStore.edit { preferences ->
                preferences.remove(stringPreferencesKey(ConfigStoreKeys.HOST_ADDR))
            }
        }
    }
}

internal object HostAddressNormalizer {
    fun normalize(source: String): Result<String> = runCatching {
        val value = source.trim().removeSurrounding("\"").trim()
        require(value.isNotBlank()) { "地址不能为空" }
        require(!value.contains(' ') && !value.contains('\t')) { "地址不能包含空白字符" }

        val url = value.toHttpUrlOrNull() ?: "http://$value".toHttpUrlOrNull()
        requireNotNull(url) { "无效服务器地址：$value" }
        require(url.username.isEmpty() && url.password.isEmpty()) { "地址不能包含用户信息" }
        require(url.query == null && url.fragment == null) { "地址只能包含主机、端口和协议" }
        require(url.pathSegments == listOf("")) { "地址不能包含路径" }

        value.trimEnd('/')
    }

    fun toBaseUrl(source: String): String {
        val value = source.trim().removeSurrounding("\"").trim().trimEnd('/')
        val url = value.toHttpUrlOrNull() ?: "http://$value".toHttpUrlOrNull()
        return checkNotNull(url).toString()
    }
}
