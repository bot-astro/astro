package space.astro.bot.core.extentions

fun String.toLink(link: String) = "[$this]($link)"
fun String.toLinkWithName(name: String) = "[$name]($this)"