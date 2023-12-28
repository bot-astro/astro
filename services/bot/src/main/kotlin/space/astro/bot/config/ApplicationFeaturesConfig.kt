package space.astro.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("application.features")
class ApplicationFeaturesConfig {
    var premiumRestrictions: Boolean = true
    var generatorCooldown: Long = 3000
}