package space.astro.bot.interactions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import space.astro.bot.ui.Containers

/**
 * Thin wrapper over an interaction used by handlers to respond
 *
 * Stateless: whether the interaction was already acknowledged is read from JDA
 */
class InteractionResponder(
    private val callback: IReplyCallback,
    private val ack: Ack,
) {
    /**
     * Sends [container] as the response
     *
     * If the interaction was acknowledged it edits the original response (the deferred reply or the message
     * of the component), otherwise it replies ephemerally
     */
    suspend fun reply(container: Container) {
        if (!callback.isAcknowledged) {
            callback
                .replyComponents(container)
                .setEphemeral(true)
                .await()
            return
        }

        callback.hook
            .editOriginalComponents(container)
            .await()
    }

    /**
     * Sends [container] as an additional ephemeral message, leaves the original response untouched
     *
     * @throws IllegalStateException if the interaction was not acknowledged
     */
    suspend fun followUp(container: Container) {
        check(callback.isAcknowledged) { "Cannot send a follow up to an interaction that was not acknowledged" }

        callback.hook
            .sendMessageComponents(container)
            .setEphemeral(true)
            .await()
    }

    /**
     * Replies with an error without destroying a message that should stay as it is
     * (component edits keep the message and get an ephemeral follow up)
     */
    suspend fun error(description: String) {
        val container = Containers.error(description)

        if (callback.isAcknowledged && (ack == Ack.DEFER_EDIT || ack == Ack.NONE)) {
            followUp(container)
        } else {
            reply(container)
        }
    }
}
