package space.astro.bot.interactions

import dev.minn.jda.ktx.coroutines.await
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.guards.CooldownGuard
import space.astro.bot.interactions.guards.InteractionGuard
import space.astro.bot.interactions.guards.InteractionRequest
import space.astro.bot.interactions.handlers.InteractionHandler
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.bot.utils.extensions.toConfigurationErrorDto
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository

private val log = KotlinLogging.logger { }

/**
 * The flow shared by every interaction:
 * guild checks → cooldown → guild settings → guards → context resolution → acknowledge → handler → error mapping
 *
 * Everything before the acknowledgement must stay fast (Discord expects a response within 3 seconds)
 */
@Component
class InteractionPipeline(
    private val cooldownGuard: CooldownGuard,
    // already ordered by @Order
    private val guards: List<InteractionGuard>,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) {
    suspend fun <E, C : InteractionContext> run(
        event: E,
        key: String,
        handler: InteractionHandler<C>,
        block: suspend (C) -> Unit,
    ) where E : GenericInteractionCreateEvent, E : IReplyCallback {
        val settings = handler.settings
        var responder = InteractionResponder(event, settings.ack, L.default)

        val guild = event.guild
        val member = event.member

        if (guild == null || member == null) {
            responder.error("error.outside_guild")
            return
        }

        try {
            // runs before the guild settings are retrieved so a user on cooldown costs no database read
            cooldownGuard.check(key, member, settings)

            val guildSettings = retrieveGuildSettings(guild.id)
            responder = InteractionResponder(event, settings.ack, guildSettings.supportedLocale)

            val request = InteractionRequest(key, guild, member, guildSettings, settings)
            guards.forEach { it.check(request) }

            val ctx =
                handler.contextResolver.resolve(
                    event,
                    InteractionContext(guild, member, guildSettings, responder),
                )

            acknowledge(event, settings.ack)

            block(ctx)
        } catch (e: InteractionException) {
            responder.error(e.key, *e.args)
        } catch (e: ConfigurationException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(e.configurationErrorEntity)
            responder.error("error.invalid_configuration", "description" to e.configurationErrorEntity.description)
        } catch (e: InsufficientPermissionException) {
            val configurationError = e.toConfigurationErrorDto(guild.id)

            configurationErrorEventPublisher.publishConfigurationErrorEvent(configurationError)
            responder.error("error.missing_permissions", "description" to configurationError.description)
        } catch (e: Exception) {
            log.error(e) { "Unhandled exception in interaction $key (guild ${guild.id})" }

            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                ConfigurationErrorEntity(guildId = guild.id, description = e.toString()),
            )
            responder.error("error.unknown")
        }
    }

    private suspend fun retrieveGuildSettings(guildId: String): GuildSettingsEntity? =
        withContext(Dispatchers.IO) { guildSettingsRepository.findByIdOrNull(guildId) }

    private suspend fun acknowledge(
        event: IReplyCallback,
        ack: Ack,
    ) {
        when (ack) {
            Ack.DEFER_REPLY -> event.deferReply(false).await()
            Ack.DEFER_REPLY_EPHEMERAL -> event.deferReply(true).await()
            Ack.DEFER_EDIT -> {
                val editCallback =
                    event as? IMessageEditCallback
                        ?: throw IllegalStateException("Ack.DEFER_EDIT is not supported by ${event::class.simpleName}")
                editCallback.deferEdit().await()
            }
            Ack.NONE -> {}
        }
    }
}
