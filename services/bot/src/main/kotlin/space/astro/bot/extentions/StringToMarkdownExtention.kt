package space.astro.bot.extentions

fun String.toLink(link: String) = "[$this]($link)"
fun String.toLinkWithName(name: String) = "[$name]($this)"