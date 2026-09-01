package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("frontend")
data class FrontendProperties(
    val baseUrl: String
)
