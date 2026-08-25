package space.astro.api.central

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import space.astro.shared.core.properties.DiscordOAuthProperties

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = [
    "space.astro.shared.core.properties.api_central",
    "space.astro.api.central"
])
@EnableConfigurationProperties(DiscordOAuthProperties::class)
class Application

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false

    runApplication<Application>(*args)
}
