package space.astro.bot.utils.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.utils.extensions.toConfigurationErrorDto
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class DefaultFailureConsumer(
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) : Consumer<Throwable> {

    val log = KotlinLogging.logger { }

    override fun accept(t: Throwable) {
        if (t is TimeoutException || t is RejectedExecutionException) {
            return
        }

        if (t is InsufficientPermissionException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                configurationErrorEntity = t.toConfigurationErrorDto(t.guildId.toString())
            )
            return
        }

        log.error(t) { "RestAction returned failure" }
    }
}