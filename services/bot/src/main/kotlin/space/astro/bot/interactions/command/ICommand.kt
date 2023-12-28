package space.astro.bot.interactions.command

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import kotlin.reflect.KFunction

interface ICommand {

    fun handleAutoComplete(event: CommandAutoCompleteInteractionEvent)

    val data: CommandData
    val commands: MutableMap<String, Pair<KFunction<*>, List<String>>>
    val category: CommandCategory
    var id: Long?
}
