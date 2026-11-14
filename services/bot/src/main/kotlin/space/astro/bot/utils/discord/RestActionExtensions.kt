package space.astro.bot.utils.discord

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction

/**
 * Awaits a channel deletion, a channel that is already deleted counts as a success
 */
suspend fun RestAction<*>.awaitChannelDeletion() {
    try {
        await()
    } catch (e: ErrorResponseException) {
        if (e.errorResponse != ErrorResponse.UNKNOWN_CHANNEL) {
            throw e
        }
    }
}
