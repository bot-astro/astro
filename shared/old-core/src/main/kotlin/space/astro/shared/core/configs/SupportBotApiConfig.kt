package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Api configuration variables for the support-bot module
 *
 * @property baseUrl
 * @property originUrl
 * @property auth
 */
@Configuration
@ConfigurationProperties("support.bot")
class SupportBotApiConfig {
    var enabled: Boolean = true
    var baseUrl: String = "http://localhost:9001"
    var originUrl: String = "http://localhost:3000"
    var auth: String = "authtoken"

}