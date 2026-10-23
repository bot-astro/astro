package space.astro.bot.interactions

/**
 * Thrown (by guards, context resolvers or handlers) when something is wrong in a way the user should be told about,
 * the message is sent back as an error reply
 */
class InteractionException(
    override val message: String,
) : Exception(message)
