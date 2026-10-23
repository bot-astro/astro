package space.astro.bot.interactions

import org.springframework.stereotype.Component
import space.astro.bot.interactions.handlers.AutocompleteHandler
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.interactions.handlers.EntitySelectHandler
import space.astro.bot.interactions.handlers.ModalHandler
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.interactions.handlers.StringSelectHandler

/**
 * Lookup of all interaction handlers, fails at startup on duplicated keys
 */
@Component
class InteractionRegistry(
    slashHandlers: List<SlashHandler<*>>,
    buttonHandlers: List<ButtonHandler<*>>,
    stringSelectHandlers: List<StringSelectHandler<*>>,
    entitySelectHandlers: List<EntitySelectHandler<*>>,
    modalHandlers: List<ModalHandler<*>>,
    autocompleteHandlers: List<AutocompleteHandler>,
) {
    val slash = index("slash command", slashHandlers) { it.data.name }
    val buttons = index("button", buttonHandlers) { it.id }
    val stringSelects = index("string select", stringSelectHandlers) { it.id }
    val entitySelects = index("entity select", entitySelectHandlers) { it.id }
    val modals = index("modal", modalHandlers) { it.id }
    val autocompletes = index("autocomplete", autocompleteHandlers) { it.command }

    private fun <T> index(
        kind: String,
        handlers: List<T>,
        key: (T) -> String,
    ): Map<String, T> {
        val map = HashMap<String, T>()

        handlers.forEach { handler ->
            val handlerKey = key(handler)
            check(map.put(handlerKey, handler) == null) { "Found duplicated $kind key: $handlerKey" }
        }

        return map
    }
}
