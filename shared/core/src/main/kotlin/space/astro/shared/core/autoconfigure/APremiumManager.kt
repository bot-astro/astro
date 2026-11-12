package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.properties.PremiumProperties

@AutoConfiguration
@EnableConfigurationProperties(PremiumProperties::class)
@ConditionalOnProperty(
    prefix = "premium",
    name = ["server-sku-id"],
)
class APremiumManager {
    @Bean
    @ConditionalOnMissingBean
    fun premiumManager(premiumProperties: PremiumProperties) = PremiumManager(premiumProperties)
}
