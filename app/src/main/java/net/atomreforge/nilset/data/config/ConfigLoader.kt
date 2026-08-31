package net.atomreforge.nilset.data.config

import android.content.Context
import com.charleskorn.kaml.Yaml

/**
 * 配置加载器。
 * 优先探测 assets 中的 config.test.yaml（临时覆盖用），否则加载 config.yaml。
 * 加载或解析失败直接抛异常，fail-fast，不带病运行。
 *
 * debug sourceSet 的 config.yaml 会覆盖 main 的同名文件（Android 构建系统行为），
 * 无需在此区分构建类型。
 */
object ConfigLoader {

    private const val TEST_CONFIG_FILE = "config.test.yaml"
    private const val CONFIG_FILE = "config.yaml"

    private var cached: AppConfig? = null

    fun mustLoad(context: Context): AppConfig {
        cached?.let { return it }

        val assets = context.assets
        val fileNames = assets.list("") ?: emptyArray()
        val actualFile = if (TEST_CONFIG_FILE in fileNames) TEST_CONFIG_FILE else CONFIG_FILE

        val yamlText = assets.open(actualFile).bufferedReader().use { it.readText() }

        val config = try {
            Yaml.default.decodeFromString(AppConfig.serializer(), yamlText)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse $actualFile: ${e.message}", e)
        }

        validate(config)
        cached = config
        return config
    }

    private fun validate(config: AppConfig) {
        if (config.api.baseUrl.isBlank()) {
            throw IllegalStateException("api.baseUrl must not be blank")
        }
        if (!config.api.baseUrl.startsWith("http://") && !config.api.baseUrl.startsWith("https://")) {
            throw IllegalStateException("api.baseUrl must start with http:// or https://, got: '${config.api.baseUrl}'")
        }
        DurationParser.parse(config.api.timeouts.connect)
        DurationParser.parse(config.api.timeouts.read)
    }
}
