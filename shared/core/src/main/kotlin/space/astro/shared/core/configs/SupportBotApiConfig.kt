package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("premium.api")
class SupportBotApiConfig {

    var baseUrl: String = "http://localhost:9001"
    var originUrl: String = "http://localhost:3000"
    var auth: String = "password"

}