package space.astro.bot.utils.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.utils.extensions.toConfigurationErrorDto
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class DefaultFailureConsumer(
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) : Consumer<Throwable> {
    val log = KotlinLogging.logger { }

    private companion object {
        val IGNORED_ERROR_RESPONSES =
            setOf(
                ErrorResponse.UNKNOWN_CHANNEL,
                ErrorResponse.UNKNOWN_MEMBER,
                ErrorResponse.UNKNOWN_INTERACTION,
                ErrorResponse.INVALID_WEBHOOK_TOKEN,
            )
    }

    override fun accept(t: Throwable) {
        if (t is TimeoutException || t is RejectedExecutionException) {
            return
        }

        if (t is InsufficientPermissionException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                configurationErrorEntity = t.toConfigurationErrorDto(t.guildId.toString()),
            )
            return
        }

        // expected races: the entity was deleted / the interaction expired before the request was executed
        if (t is ErrorResponseException && t.errorResponse in IGNORED_ERROR_RESPONSES) {
            log.debug { "Ignored RestAction failure: ${t.meaning}" }
            return
        }

        log.error(t) { "RestAction returned failure" }
    }
}
