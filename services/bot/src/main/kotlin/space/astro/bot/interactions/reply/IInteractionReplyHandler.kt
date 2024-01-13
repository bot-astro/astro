package space.astro.bot.interactions.reply

import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IPremiumReplyCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.modals.Modal

interface IInteractionReplyHandler {
    /**
     * Sets a new reply callback.
     *
     * Should be used when the event the interaction is reacting to is changed
     */
    fun setReplyCallback(replyCallback: IReplyCallback)

    /**
     * Sets a new modal callback.
     *
     * Should be used when the event the interaction is reacting to is changed
     */
    fun setModalCallback(modalCallback: IModalCallback?)

    /**
     * Sets a new premium reply callback.
     *
     * Should be used when the event the interaction is reacting to is changed
     */
    fun setPremiumReplyCallback(premiumReplyCallback: IPremiumReplyCallback?)

    /**
     * Sets the interaction replies as ephemeral
     *
     * @throws IllegalStateException if the interaction was already replied to
     */
    fun setEphemeral(ephemeral: Boolean)

    /**
     * Defers the reply to this interaction
     *
     * @throws IllegalStateException if the interaction was already replied to
     */
    suspend fun deferReply()

    suspend fun reply(
        embed: MessageEmbed,
        components: List<LayoutComponent>
    )

    suspend fun replyEmbed(embed: MessageEmbed)

    suspend fun replyEmbedAndComponent(embed: MessageEmbed, component: ItemComponent)

    /**
     * @throws IllegalStateException if editing components on an interaction that has not been replied to yet
     */
    suspend fun editComponents(components: List<LayoutComponent>)

    /**
     * @throws IllegalStateException if the interaction doesn't support modal replies
     */
    suspend fun replyModal(modal: Modal)

    /**
     * @throws IllegalStateException if the interaction doesn't support premium required replies
     */
    suspend fun replyPremiumRequired()
}