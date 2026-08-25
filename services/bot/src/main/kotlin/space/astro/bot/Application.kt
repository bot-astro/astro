package space.astro.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.messages.MessageRequest.setDefaultUseComponentsV2
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import space.astro.bot.utils.discord.DefaultFailureConsumer
import space.astro.shared.core.properties.ShardManagerConfig

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(ShardManagerConfig::class)
class Application

fun main(args: Array<String>) {
    KotlinLoggingConfiguration.logStartupMessage = false
    val log = KotlinLogging.logger { }

    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        log.error(e) { "Uncaught exception in thread $t" }
    }

    setDefaultUseComponentsV2(true)

    runApplication<Application>(*args)
}