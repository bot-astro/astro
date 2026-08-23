package space.astro.api.central.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("bot.endpoint")
class BotEndpointConfig {
    var localhost: Boolean = true
    var port: Long = 9000
    var podName: String = "astro"
    var serviceName: String = "bot"
    var namespace: String = "astro"
}
