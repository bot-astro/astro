package space.astro.bot.interactions.menu

import space.astro.bot.interactions.InteractionAction
import kotlin.reflect.KFunction

interface IMenu {
    var id: String
    var runnable: KFunction<*>?
    val action: InteractionAction
}