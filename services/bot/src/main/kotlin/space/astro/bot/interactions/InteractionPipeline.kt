package space.astro.bot.interactions

import dev.minn.jda.ktx.coroutines.await
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
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
import space.astro.shared.core.lang.localizedDescription
import space.astro.shared.core.models.database.ConfigurationErrorCode
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import java.time.Duration

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
    private val registry: MeterRegistry,
) {
    suspend fun <E, C : InteractionContext> run(
        event: E,
        key: String,
        handler: InteractionHandler<C>,
        block: suspend (C) -> Unit,
    ) where E : GenericInteractionCreateEvent, E : IReplyCallback {
        val sample = Timer.start(registry)

        // /////////////
        // / EXECUTE ///
        // /////////////
        val outcome = execute(event, key, handler, block)

        // /////////////
        // / METRICS ///
        // /////////////
        val type = event.type.name.lowercase()

        Counter
            .builder("astro.interactions")
            .tag("type", type)
            .tag("name", key)
            .tag("outcome", outcome)
            .register(registry)
            .increment()

        sample.stop(
            Timer
                .builder("astro.interactions.duration")
                .tag("type", type)
                .tag("name", key)
                .serviceLevelObjectives(
                    Duration.ofMillis(100),
                    Duration.ofMillis(250),
                    Duration.ofMillis(500),
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(3),
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(10),
                ).register(registry),
        )
    }

    /**
     * Runs the shared flow and replies to the user if something goes wrong
     *
     * @return the outcome used as metric tag: ok, outside_guild, user_error, config_error, no_permission or failed
     */
    private suspend fun <E, C : InteractionContext> execute(
        event: E,
        key: String,
        handler: InteractionHandler<C>,
        block: suspend (C) -> Unit,
    ): String where E : GenericInteractionCreateEvent, E : IReplyCallback {
        val settings = handler.settings
        var responder = InteractionResponder(event, settings.ack, L.default)

        // /////////////////
        // / GUILD CHECK ///
        // /////////////////
        val guild = event.guild
        val member = event.member

        if (guild == null || member == null) {
            responder.error("error.outside_guild")
            return "outside_guild"
        }

        return try {
            // //////////////
            // / COOLDOWN ///
            // //////////////
            // runs before the guild settings are retrieved so a user on cooldown costs no database read
            cooldownGuard.check(key, member, settings)

            // ////////////////////
            // / GUILD SETTINGS ///
            // ////////////////////
            val guildSettings = retrieveGuildSettings(guild.id)
            responder = InteractionResponder(event, settings.ack, guildSettings.supportedLocale)

            // ////////////
            // / GUARDS ///
            // ////////////
            val request = InteractionRequest(key, guild, member, guildSettings, settings)
            guards.forEach { it.check(request) }

            // /////////////
            // / CONTEXT ///
            // /////////////
            val ctx =
                handler.contextResolver.resolve(
                    event,
                    InteractionContext(guild, member, guildSettings, responder),
                )

            // /////////////////
            // / ACKNOWLEDGE ///
            // /////////////////
            acknowledge(event, settings.ack)

            // /////////////
            // / EXECUTE ///
            // /////////////
            block(ctx)

            "ok"
        } catch (e: InteractionException) {
            responder.error(e.key, *e.args)
            "user_error"
        } catch (e: ConfigurationException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(e.configurationErrorEntity)
            responder.error(
                "error.invalid_configuration",
                "description" to e.configurationErrorEntity.localizedDescription(responder.locale),
            )
            "config_error"
        } catch (e: InsufficientPermissionException) {
            val configurationError = e.toConfigurationErrorDto(guild.id)

            configurationErrorEventPublisher.publishConfigurationErrorEvent(configurationError)
            responder.error("error.missing_permissions", "description" to configurationError.localizedDescription(responder.locale))
            "no_permission"
        } catch (e: Exception) {
            log.error(e) { "Unhandled exception in interaction $key (guild ${guild.id})" }

            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                ConfigurationErrorEntity(
                    guildId = guild.id,
                    code = ConfigurationErrorCode.UNKNOWN,
                    args = mapOf("encounteredIn" to "interaction $key", "details" to e.toString()),
                ),
            )
            responder.error("error.unknown")
            "failed"
        }
    }

    private suspend fun retrieveGuildSettings(guildId: String): GuildSettingsEntity? =
        withContext(Dispatchers.IO) { guildSettingsRepository.findByGuildID(guildId) }

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
