package space.astro.api.central

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import space.astro.shared.core.properties.DiscordOAuthProperties

@SpringBootApplication(
    exclude = [
        UserDetailsServiceAutoConfiguration::class
    ]
)
@ConfigurationPropertiesScan(basePackages = [
    "space.astro.shared.core.properties.api_central",
    "space.astro.api.central"
])
@EnableConfigurationProperties(DiscordOAuthProperties::class)
@OpenAPIDefinition(
    info = Info(
        title = "Astro API",
        version = "2.0",
        description = "Astro REST API mainly used by the web dashboard."
    )
)
class Application

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false

    runApplication<Application>(*args)
}
