package space.astro.bot.interactions

/**
 * Thrown (by guards, context resolvers or handlers) when something is wrong in a way the user should be told about,
 * [key] is the translation key of the message sent back as an error reply, formatted with [args]
 */
class InteractionException(
    val key: String,
    vararg val args: Pair<String, Any?>,
) : Exception(key)
