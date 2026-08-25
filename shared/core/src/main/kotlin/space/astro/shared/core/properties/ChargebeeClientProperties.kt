package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chargebee")
data class ChargebeeClientProperties(
    val enabled: Boolean,
    val siteName: String,
    val apiKey: String,
    val serverUltimatePlanId: String
)