package space.astro.bot.interactions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import space.astro.bot.ui.Containers
import space.astro.shared.core.lang.L
import java.util.Locale

/**
 * Thin wrapper over an interaction used by handlers to respond
 *
 * Stateless: whether the interaction was already acknowledged is read from JDA
 */
class InteractionResponder(
    private val callback: IReplyCallback,
    private val ack: Ack,
    val locale: Locale,
) {
    fun t(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String = L.t(locale, key, *args)

    /**
     * Sends [components] as the response
     *
     * If the interaction was acknowledged it edits the original response (the deferred reply or the message
     * of the component), otherwise it replies, ephemerally unless [ephemeral] is false
     */
    suspend fun reply(
        vararg components: MessageTopLevelComponent,
        ephemeral: Boolean = true,
    ) {
        if (!callback.isAcknowledged) {
            callback
                .replyComponents(components.toList())
                .setEphemeral(ephemeral)
                .await()
            return
        }

        callback.hook
            .editOriginalComponents(components.toList())
            .useComponentsV2()
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
     *
     * @param key translation key of the error message
     */
    suspend fun error(
        key: String,
        vararg args: Pair<String, Any?>,
    ) {
        val container = Containers.error(t(key, *args))

        if (callback.isAcknowledged && (ack == Ack.DEFER_EDIT || ack == Ack.NONE)) {
            followUp(container)
        } else {
            reply(container)
        }
    }
}
