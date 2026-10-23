package space.astro.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling
import space.astro.shared.core.properties.PremiumProperties
import space.astro.shared.core.properties.ShardManagerConfig

@SpringBootApplication(
    exclude = [
        UserDetailsServiceAutoConfiguration::class,
    ],
)
@ConfigurationPropertiesScan(
    basePackages = [
        "space.astro.shared.core.properties.bot",
        "space.astro.bot.properties",
    ],
)
@EnableConfigurationProperties(ShardManagerConfig::class, PremiumProperties::class)
@EnableScheduling
class Application

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false
    val log = KotlinLogging.logger { }

    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        log.error(e) { "Uncaught exception in thread $t" }
    }

    runApplication<Application>(*args)
}
