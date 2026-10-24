package space.astro.bot.events.listeners.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionPipeline
import space.astro.bot.interactions.InteractionRegistry
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.interactions.handlers.EntitySelectHandler
import space.astro.bot.interactions.handlers.ModalHandler
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.interactions.handlers.StringSelectHandler
import space.astro.bot.interactions.handlers.UserContextHandler
import space.astro.bot.models.analytics.SlashCommandInvocationEventData
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val log = KotlinLogging.logger { }

/**
 * Routes JDA interaction events to the matching handler through the [InteractionPipeline]
 */
@Component
class InteractionEventListener(
    private val registry: InteractionRegistry,
    private val pipeline: InteractionPipeline,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val jsonMapper: JsonMapper,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun onSlashCommand(event: SlashCommandInteractionEvent) {
        val handler = registry.slash[event.name] ?: return
        scope.launch { runSlash(handler, event) }
    }

    @EventListener
    fun onUserContext(event: UserContextInteractionEvent) {
        val handler = registry.userContexts[event.name] ?: return
        scope.launch { runUserContext(handler, event) }
    }

    @EventListener
    fun onButton(event: ButtonInteractionEvent) {
        val key = event.componentId.substringBefore('?')
        val handler = registry.buttons[key] ?: return
        scope.launch { runButton(handler, key, event) }
    }

    @EventListener
    fun onStringSelect(event: StringSelectInteractionEvent) {
        val key = event.componentId.substringBefore('?')
        val handler = registry.stringSelects[key] ?: return
        scope.launch { runStringSelect(handler, key, event) }
    }

    @EventListener
    fun onEntitySelect(event: EntitySelectInteractionEvent) {
        val key = event.componentId.substringBefore('?')
        val handler = registry.entitySelects[key] ?: return
        scope.launch { runEntitySelect(handler, key, event) }
    }

    @EventListener
    fun onModal(event: ModalInteractionEvent) {
        val key = event.modalId.substringBefore('?')
        val handler = registry.modals[key] ?: return
        scope.launch { runModal(handler, key, event) }
    }

    @EventListener
    fun onAutocomplete(event: CommandAutoCompleteInteractionEvent) {
        val handler = registry.autocompletes[event.fullCommandName.replace(' ', '.')] ?: return

        scope.launch {
            try {
                handler.handle(event)
            } catch (e: Exception) {
                log.error(e) { "Unhandled exception in autocomplete of ${event.fullCommandName}" }
            }
        }
    }

    private suspend fun <C : InteractionContext> runSlash(
        handler: SlashHandler<C>,
        event: SlashCommandInteractionEvent,
    ) = pipeline.run(event, event.name, handler) { ctx ->
        publishSlashCommandInvocation(event)
        handler.handle(event, ctx)
    }

    private fun publishSlashCommandInvocation(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val options = event.options

        applicationEventPublisher.publishEvent(
            SlashCommandInvocationEventData(
                name = event.fullCommandName.replace(' ', '.'),
                guildId = guild.idLong,
                channelId = event.channelIdLong,
                userId = event.user.idLong,
                mainOptionName = options.firstOrNull()?.name,
                mainOptionValue = options.firstOrNull()?.asString,
                rawOptions =
                    options
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                            jsonMapper.writeValueAsString(
                                mapOf(
                                    "options" to it.map { o -> mapOf("name" to o.name, "value" to o.asString) },
                                ),
                            )
                        },
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )
    }

    private suspend fun <C : InteractionContext> runUserContext(
        handler: UserContextHandler<C>,
        event: UserContextInteractionEvent,
    ) = pipeline.run(event, event.name, handler) { ctx -> handler.handle(event, ctx) }

    private suspend fun <C : InteractionContext> runButton(
        handler: ButtonHandler<C>,
        key: String,
        event: ButtonInteractionEvent,
    ) = pipeline.run(event, key, handler) { ctx -> handler.handle(event, ctx) }

    private suspend fun <C : InteractionContext> runStringSelect(
        handler: StringSelectHandler<C>,
        key: String,
        event: StringSelectInteractionEvent,
    ) = pipeline.run(event, key, handler) { ctx -> handler.handle(event, ctx) }

    private suspend fun <C : InteractionContext> runEntitySelect(
        handler: EntitySelectHandler<C>,
        key: String,
        event: EntitySelectInteractionEvent,
    ) = pipeline.run(event, key, handler) { ctx -> handler.handle(event, ctx) }

    private suspend fun <C : InteractionContext> runModal(
        handler: ModalHandler<C>,
        key: String,
        event: ModalInteractionEvent,
    ) = pipeline.run(event, key, handler) { ctx -> handler.handle(event, ctx) }
}
