package space.astro.support.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("pod")
class PodConfig {

    var hostname: String = "support-01.astro-bot.space"
    var ordinal: String = "support-0"

    fun getParsedOrdinal() : Int {
        return ordinal.split("-").last().toInt()
    }

}
