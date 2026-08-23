package space.astro.api.central.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("central.api")
class CentralApiConfig {
    var auth: String = "password"
    var sessionCookieName: String = "astro-session"
    var sessionCookieAllowOrigin: String = "http://localhost:3000"
    var sessionCookieDomain: String? = null
    var sessionCookieSecure: Boolean = true
    var sessionCookieHttpOnly: Boolean = false
    var sessionCookieMaxAgeInSeconds: Long = 2592000
    var sessionCookieSameSite: String = "None"
    var corsAllowedHeaders: String = "content-type, origin, accept, authorization"
    var corsAllowedMethods: String = "GET, POST, PUT, DELETE, OPTIONS, HEAD"
}
