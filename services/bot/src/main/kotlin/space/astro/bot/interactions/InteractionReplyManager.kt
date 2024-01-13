package space.astro.bot.interactions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.LayoutComponent

class InteractionReplyManager(
    private var replyCallback: IReplyCallback,
    private val originatedFromInterface: Boolean,
    private val originatedFromExistingMessage: Boolean
) {
    private var ephemeralLocked = false
    private var deferringLocked = false
    private var sentFirstMessage = false

    /**
     * Tracks that the bot is replying to the interaction
     *
     * @return whether the bot should edit a previous reply or create a new one
     */
    private fun trackReply(): Boolean {
        val shouldEdit = if (originatedFromInterface) {
            sentFirstMessage
        } else if (originatedFromExistingMessage) {
            true
        } else {
            sentFirstMessage
        }

        sentFirstMessage = true
        ephemeralLocked = true
        deferringLocked = true

        return shouldEdit
    }

    private var interactionHook: InteractionHook = replyCallback.hook
    private var ephemeral: Boolean = true

    fun setReplyCallback(replyCallback: IReplyCallback) {
        this.replyCallback = replyCallback
        interactionHook = replyCallback.hook
        ephemeralLocked = false
        deferringLocked = false
    }

    /**
     * Sets the interaction replies as ephemeral
     *
     * **Can only be called if the interaction wasn't replied to yet**
     */
    fun setEphemeral(ephemeral: Boolean) {
        if (ephemeralLocked) {
            throw IllegalStateException("Trying to change ephemeral state on an interaction that was already replied to")
        }

        this.ephemeral = ephemeral
    }

    /**
     * Defers the reply to this interaction
     *
     * **Can only be called if the interaction wasn't replied to yet**
     */
    suspend fun deferReply() {
        if (deferringLocked) {
            throw IllegalStateException("Trying to defer a reply on an interaction that was already replied to")
        }

        trackReply()
        replyCallback.deferReply(ephemeral).await()
    }

    suspend fun reply(
        embed: MessageEmbed,
        components: List<LayoutComponent>
    ) {
        val shouldEdit = trackReply()

        if (!shouldEdit) {
            replyCallback.replyEmbeds(embed)
                .setComponents(components)
                .await()
        } else {
            interactionHook.editOriginalEmbeds(embed)
                .setComponents(components)
                .await()
        }
    }

    suspend fun replyEmbed(embed: MessageEmbed) {
        reply(embed, emptyList())
    }

    suspend fun editComponents(components: List<LayoutComponent>) {
        val shouldEdit = trackReply()

        if (!shouldEdit) {
            throw IllegalStateException("Tried to edit components on an interaction that has not been replied to yet")
        }

        interactionHook.editOriginalComponents(components).await()
    }
}