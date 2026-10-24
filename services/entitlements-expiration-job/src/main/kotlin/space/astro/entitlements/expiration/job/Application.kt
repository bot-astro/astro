package space.astro.entitlements.expiration.job

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import space.astro.entitlements.expiration.job.properties.DiscordApplicationProperties
import kotlin.system.exitProcess

@SpringBootApplication
@EnableConfigurationProperties(DiscordApplicationProperties::class)
class Application

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false

    // the job runs at startup (see EntitlementsExpirationRunner), the process must then exit even if connections are still open
    val exitCode =
        try {
            SpringApplication.exit(
                runApplication<Application>(*args),
            )
        } catch (_: Exception) {
            // already logged by Spring, a non zero code makes Kubernetes retry the job
            1
        }

    exitProcess(exitCode)
}
