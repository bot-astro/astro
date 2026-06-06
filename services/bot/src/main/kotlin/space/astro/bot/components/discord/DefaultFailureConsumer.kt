package space.astro.bot.components.discord

import mu.KotlinLogging
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
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
                configurationErrorData = t.toConfigurationErrorDto(t.guildId.toString())
            )
            return
        }

        log.error(t) { "RestAction returned failure" }
    }
}