package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @param enabled Whether the bot statistics are pushed to top.gg
 * @param token top.gg project token
 * @param intervalHours How often the statistics are pushed, top.gg rate limits bots strictly so keep it high
 */
@ConfigurationProperties("topgg")
data class TopGGProperties(
    val enabled: Boolean = false,
    val token: String = "",
    val baseUrl: String = "https://top.gg",
    val intervalHours: Long = 12,
)
