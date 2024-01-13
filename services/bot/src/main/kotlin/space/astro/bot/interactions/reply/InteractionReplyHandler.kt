package space.astro.bot.interactions.reply

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IPremiumReplyCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.modals.Modal

class InteractionReplyHandler(
    private var replyCallback: IReplyCallback,
    private var modalCallback: IModalCallback?,
    private var premiumReplyCallback: IPremiumReplyCallback?,
    private val originatedFromInterface: Boolean,
    private val originatedFromExistingMessage: Boolean
) : IInteractionReplyHandler {
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

    override fun setReplyCallback(replyCallback: IReplyCallback) {
        this.replyCallback = replyCallback
        interactionHook = replyCallback.hook
        ephemeralLocked = false
        deferringLocked = false
    }

    override fun setModalCallback(modalCallback: IModalCallback?) {
        this.modalCallback = modalCallback
    }

    override fun setPremiumReplyCallback(premiumReplyCallback: IPremiumReplyCallback?) {
        this.premiumReplyCallback = premiumReplyCallback
    }

    override fun setEphemeral(ephemeral: Boolean) {
        if (ephemeralLocked) {
            throw IllegalStateException("Trying to change ephemeral state on an interaction that was already replied to")
        }

        this.ephemeral = ephemeral
    }


    override suspend fun deferReply() {
        if (deferringLocked) {
            throw IllegalStateException("Trying to defer a reply on an interaction that was already replied to")
        }

        trackReply()
        replyCallback.deferReply(ephemeral).await()
    }

    override suspend fun reply(
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

    override suspend fun replyEmbed(embed: MessageEmbed) {
        reply(embed, emptyList())
    }

    override suspend fun replyEmbedAndComponent(embed: MessageEmbed, component: ItemComponent) {
        reply(embed, listOf(ActionRow.of(component)))
    }

    override suspend fun editComponents(components: List<LayoutComponent>) {
        val shouldEdit = trackReply()

        if (!shouldEdit) {
            throw IllegalStateException("Tried to edit components on an interaction that has not been replied to yet")
        }

        interactionHook.editOriginalComponents(components).await()
    }

    override suspend fun replyModal(modal: Modal) {
        if (modalCallback == null) {
            throw IllegalStateException("This interaction doesn't support modal replies")
        }

        modalCallback?.replyModal(modal)?.await()
    }

    override suspend fun replyPremiumRequired() {
        if (premiumReplyCallback == null) {
            throw IllegalStateException("This interaction doesn't support premium required replies")
        }

        premiumReplyCallback?.replyWithPremiumRequired()?.await()
    }
}