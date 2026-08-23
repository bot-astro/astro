package space.astro.shared.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chargebee")
data class ChargebeeClientConfig(
    val enabled: Boolean,
    val siteName: String,
    val apiKey: String,
    val serverPremiumPlanId: String
)