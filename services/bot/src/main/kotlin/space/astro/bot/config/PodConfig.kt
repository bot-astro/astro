package space.astro.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("pod")
class PodConfig {

    val hostname: String = "worker-01.astro-bot.space"
    val ordinal: String = "astro-0"

    fun getParsedOrdinal() : Int {
        return ordinal.split("-").last().toInt()
    }

}
