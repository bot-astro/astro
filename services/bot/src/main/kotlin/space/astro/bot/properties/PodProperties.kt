package space.astro.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("pod")
class PodProperties {
    var hostname: String = "worker-01.astro-bot.space"
    var ordinal: String = "astro-0"

    fun getParsedOrdinal(): Int = ordinal.split("-").last().toInt()
}
