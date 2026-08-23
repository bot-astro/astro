package space.astro.shared.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("premium")
data class PremiumConfig(
    var serverSkuId: String,
    var monthlyPlanId: String,
    var yearlyPlanId: String
)