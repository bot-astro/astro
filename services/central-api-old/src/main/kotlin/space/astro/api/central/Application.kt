package space.astro.api.central

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import space.astro.shared.core.properties.DiscordApplicationProperties

@SpringBootApplication
@EnableConfigurationProperties(DiscordApplicationProperties::class)
class Application

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false

    runApplication<Application>(*args)
}
