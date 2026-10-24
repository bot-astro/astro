package space.astro.support.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

@SpringBootApplication(
    exclude = [
        UserDetailsServiceAutoConfiguration::class,
    ],
    // the support bot doesn't use the database
    excludeName = [
        "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration",
    ],
)
@ConfigurationPropertiesScan(
    basePackages = [
        "space.astro.support.bot.properties",
    ],
)
class Application

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false
    val log = KotlinLogging.logger { }

    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        log.error(e) { "Uncaught exception in thread $t" }
    }

    runApplication<Application>(*args)
}
