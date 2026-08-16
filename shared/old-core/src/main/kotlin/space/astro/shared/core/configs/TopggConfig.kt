package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("topgg")
class TopggConfig {
    var webhookAuth: String = "secret"
    var token: String = "secret"
}