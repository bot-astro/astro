package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("premium.features")
class PremiumFeaturesConfig {
    var premiumRestrictions: Boolean = true
    var premiumServerSkuId: String = "1096107722115661934"
    var premiumMonthlyPlanId: String = "Server-Premium-USD-Monthly"
    var premiumYearlyPlanId: String = "Server-Premium-USD-Yearly"
}