package space.astro.bot.interactions.button

import kotlin.reflect.KFunction

interface IButton {
    var id: String
    var runnable: KFunction<*>?
    var premium: Boolean
}