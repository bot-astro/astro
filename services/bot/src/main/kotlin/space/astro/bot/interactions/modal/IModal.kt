package space.astro.bot.interactions.modal

import space.astro.bot.interactions.InteractionAction
import kotlin.reflect.KFunction

interface IModal {
    var id: String
    var runnable: KFunction<*>?
    val action: InteractionAction
}