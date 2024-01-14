package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "chargebee")
class ChargebeeConfig {
    var siteName = "astro-bot"
    var webhookToken = "secret"
    var apiKey = "secret"
}