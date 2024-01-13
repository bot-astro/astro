package space.astro.bot.interactions

import net.dv8tion.jda.api.entities.MessageEmbed

/**
 * A generic exception used when building the context of an interaction fails
 * Contains an embed that describes the error
 *
 * This is not clean-code as the better flow would be to throw some specific exception
 * for each issue or an exception with an error id and then make a function to create an
 * embed for that specific error, but seen that the use case of this is to simply reply
 * to the user telling him what's wrong, it doesn't make sense to make such a system
 *
 * @param errorEmbed a [MessageEmbed] that describes the error
 */
class InteractionContextBuilderException(
    val errorEmbed: MessageEmbed
) : Exception()