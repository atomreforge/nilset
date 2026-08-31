package net.atomreforge.nilset.data.config

import java.time.Duration

/**
 * 解析 "10s" / "5m" / "1h" / "500ms" 格式的时长字符串为 [Duration]。
 * 支持单位: h, m, s, ms, us, ns（与后端 config.yaml 保持一致）。
 */
object DurationParser {

    private val pattern = Regex("(\\d+)(ms|us|ns|h|m|s)")

    fun parse(value: String): Duration {
        val match = pattern.matchEntire(value.trim())
            ?: throw IllegalArgumentException(
                "Invalid duration format: '$value'. Expected: <number><unit>, e.g. '10s', '5m', '1h', '500ms'"
            )
        val number = match.groupValues[1].toLong()
        return when (match.groupValues[2]) {
            "h" -> Duration.ofHours(number)
            "m" -> Duration.ofMinutes(number)
            "s" -> Duration.ofSeconds(number)
            "ms" -> Duration.ofMillis(number)
            "us" -> Duration.ofNanos(number * 1_000)
            "ns" -> Duration.ofNanos(number)
            else -> throw IllegalArgumentException("Unknown duration unit: '${match.groupValues[2]}'")
        }
    }
}
