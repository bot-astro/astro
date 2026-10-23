package space.astro.bot.interactions

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.interactions.handlers.ModalHandler
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.interactions.handlers.StringSelectHandler

/**
 * Routes JDA interaction events to the matching handler through the [InteractionPipeline]
 */
@Component
class InteractionEventListener(
    private val registry: InteractionRegistry,
    private val pipeline: InteractionPipeline,
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
    fun onModal(event: ModalInteractionEvent) {
        val key = event.modalId.substringBefore('?')
        val handler = registry.modals[key] ?: return
        scope.launch { runModal(handler, key, event) }
    }

    private suspend fun <C : InteractionContext> runSlash(
        handler: SlashHandler<C>,
        event: SlashCommandInteractionEvent,
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

    private suspend fun <C : InteractionContext> runModal(
        handler: ModalHandler<C>,
        key: String,
        event: ModalInteractionEvent,
    ) = pipeline.run(event, key, handler) { ctx -> handler.handle(event, ctx) }
}
