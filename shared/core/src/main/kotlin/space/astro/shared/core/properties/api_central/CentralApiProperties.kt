package space.astro.shared.core.properties.api_central

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("central.api")
data class CentralApiProperties(
    val auth: String,
    val sessionCookieName: String,
    val sessionCookieAllowOrigin: String,
    val sessionCookieDomain: String?,
    val sessionCookieSecure: Boolean,
    val sessionCookieSameSite: String,
    val corsAllowedHeaders: String,
    val corsAllowedMethods: String,
)