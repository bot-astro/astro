package space.astro.api.central.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("central.api")
class CentralApiConfig {
    var auth: String = "password"
    var sessionCookieName: String = "astro-session"
    var sessionCookieAllowOrigin: String = "http://localhost:3000"
    var sessionCookieDomain: String? = null
    var sessionCookieSecure: Boolean = false
    var sessionCookieHttpOnly: Boolean = false
    var sessionCookieMaxAgeInSeconds: Long = 2592000
    var sessionCookieSameSite: String = "None"
}
