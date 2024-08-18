package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("premium.features")
class PremiumFeaturesConfig {
    var restrictions: Boolean = true
    var serverSkuId: String = "1096107722115661934"
    var monthlyPlanId: String = "Server-Premium-USD-Monthly"
    var yearlyPlanId: String = "Server-Premium-USD-Yearly"
}