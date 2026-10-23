package space.astro.bot.interactions.context

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import space.astro.bot.interactions.InteractionException

/**
 * Builds the context a handler works with from the base context
 */
fun interface ContextResolver<C : InteractionContext> {
    /**
     * @throws InteractionException if the context cannot be built, the message is shown to the user
     */
    suspend fun resolve(
        event: GenericInteractionCreateEvent,
        base: InteractionContext,
    ): C
}

object BaseContextResolver : ContextResolver<InteractionContext> {
    override suspend fun resolve(
        event: GenericInteractionCreateEvent,
        base: InteractionContext,
    ): InteractionContext = base
}
