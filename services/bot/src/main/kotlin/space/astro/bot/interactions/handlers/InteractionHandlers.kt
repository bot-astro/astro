package space.astro.bot.interactions.handlers

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.ContextResolver
import space.astro.bot.interactions.context.InteractionContext

/**
 * Common part of all handlers
 *
 * A single class can implement more than one handler kind (e.g. a slash command and the button doing the same thing)
 * and share the same settings and context. Implement as `@Component` to get picked up.
 */
interface InteractionHandler<C : InteractionContext> {
    val settings: InteractionSettings
    val contextResolver: ContextResolver<C>
}

interface SlashHandler<C : InteractionContext> : InteractionHandler<C> {
    val data: SlashCommandData

    suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: C,
    )
}

/**
 * [id] is matched against the part of the component id before the first `?`
 */
interface ButtonHandler<C : InteractionContext> : InteractionHandler<C> {
    val id: String

    suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: C,
    )
}

/**
 * [id] is matched against the part of the component id before the first `?`
 */
interface StringSelectHandler<C : InteractionContext> : InteractionHandler<C> {
    val id: String

    suspend fun handle(
        event: StringSelectInteractionEvent,
        ctx: C,
    )
}

interface ModalHandler<C : InteractionContext> : InteractionHandler<C> {
    val id: String

    suspend fun handle(
        event: ModalInteractionEvent,
        ctx: C,
    )
}
