package space.astro.bot.interactions.menu

import kotlin.reflect.KFunction

interface IMenu {
    var id: String
    var runnable: KFunction<*>?
}