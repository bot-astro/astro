package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("premium")
data class PremiumProperties(
    var serverSkuId: String,
    var monthlyPlanId: String,
    var yearlyPlanId: String
)