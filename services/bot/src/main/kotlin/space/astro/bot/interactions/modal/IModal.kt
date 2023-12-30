package space.astro.bot.interactions.modal

import kotlin.reflect.KFunction

interface IModal {
    var id: String
    var runnable: KFunction<*>?
}