package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @param restrictions Whether the free tier limitations are enforced, when false every guild is treated as premium
 */
@ConfigurationProperties("premium")
data class PremiumProperties(
    var serverSkuId: String,
    var monthlyPlanId: String,
    var yearlyPlanId: String,
    var restrictions: Boolean = true,
)
