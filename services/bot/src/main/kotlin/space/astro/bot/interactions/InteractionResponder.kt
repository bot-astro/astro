package space.astro.bot.interactions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import space.astro.bot.ui.Embeds

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
     * Sends [embed] as the response
     *
     * If the interaction was acknowledged it edits the original response (the deferred reply or the message
     * of the component), otherwise it replies ephemerally
     */
    suspend fun reply(
        embed: MessageEmbed,
        components: List<ActionRow> = emptyList(),
    ) {
        if (!callback.isAcknowledged) {
            callback
                .replyEmbeds(embed)
                .setComponents(components)
                .setEphemeral(true)
                .await()
            return
        }

        callback.hook
            .editOriginalEmbeds(embed)
            .setComponents(components)
            .await()
    }

    /**
     * Sends [embed] as an additional ephemeral message, leaves the original response untouched
     *
     * @throws IllegalStateException if the interaction was not acknowledged
     */
    suspend fun followUp(
        embed: MessageEmbed,
        components: List<ActionRow> = emptyList(),
    ) {
        check(callback.isAcknowledged) { "Cannot send a follow up to an interaction that was not acknowledged" }

        callback.hook
            .sendMessageEmbeds(embed)
            .setComponents(components)
            .setEphemeral(true)
            .await()
    }

    /**
     * Replies with an error without destroying a message that should stay as it is
     * (component edits keep the message and get an ephemeral follow up)
     */
    suspend fun error(description: String) {
        val embed = Embeds.error(description)

        if (callback.isAcknowledged && (ack == Ack.DEFER_EDIT || ack == Ack.NONE)) {
            followUp(embed)
        } else {
            reply(embed)
        }
    }
}
