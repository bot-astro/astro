package space.astro.bot.interactions.handlers.command

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AutoCompleteHandler(
    val commandHandler: CommandHandler
) {

    @EventListener
    fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        val command = commandHandler.commandsMap[event.name] ?: return

        command.handleAutoComplete(event)
    }
}