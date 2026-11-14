package space.astro.bot.utils.discord

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction

/**
 * Awaits a channel deletion, treating an already deleted channel as a success.
 * Any other failure is rethrown so that callers can keep their state and retry later.
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
