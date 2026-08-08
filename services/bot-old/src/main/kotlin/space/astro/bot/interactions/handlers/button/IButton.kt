package space.astro.bot.interactions.handlers.button

import space.astro.bot.interactions.InteractionAction
import kotlin.reflect.KFunction

interface IButton {
    var id: String
    var runnable: KFunction<*>?
    val action: InteractionAction
}