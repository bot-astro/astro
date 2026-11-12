package space.astro.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("pod")
data class PodProperties(
    val hostname: String = "worker-01.astro-bot.space",
    val ordinal: String = "astro-0",
) {
    fun getParsedOrdinal(): Int = ordinal.split("-").last().toInt()
}
