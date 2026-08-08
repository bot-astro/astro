package space.astro.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.messages.MessageRequest.setDefaultUseComponentsV2
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import space.astro.bot.utils.discord.DefaultFailureConsumer

@SpringBootApplication
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