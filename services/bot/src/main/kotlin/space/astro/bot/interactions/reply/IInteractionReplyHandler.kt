package space.astro.bot.interactions.reply

import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IPremiumRequiredReplyCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.modals.Modal

interface IInteractionReplyHandler {
    /////////////////
    /// CALLBACKS ///
    /////////////////

    /**
     * Sets new callback.
     *
     * Should be used when the event the interaction is reacting to is changed
     */
    fun setCallbacks(
        replyCallback: IReplyCallback,
        messageEditCallback: IMessageEditCallback,
        modalCallback: IModalCallback?,
        premiumReplyCallback: IPremiumRequiredReplyCallback?,
        originatedFromInterface: Boolean = false,
        originatedFromExistingMessage: Boolean = true,
    )

    fun setCallbacksFromComponentEvent(event: GenericComponentInteractionCreateEvent)

    fun setCallbacksFromModalEvent(event: ModalInteractionEvent)


    ///////////////////////////////
    /// EPHEMERAL AND DEFERRING ///
    ///////////////////////////////

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


    ////////////////////
    /// FULL REPLIES ///
    ////////////////////
    suspend fun reply(
        embed: MessageEmbed,
        components: List<LayoutComponent>
    )


    //////////////
    /// EMBEDS ///
    //////////////
    suspend fun replyEmbed(embed: MessageEmbed)

    suspend fun replyEmbedAndComponent(embed: MessageEmbed, component: ItemComponent)

    suspend fun replyEmbedAndComponents(embed: MessageEmbed, components: List<ItemComponent>)


    //////////////////
    /// COMPONENTS ///
    //////////////////

    /**
     * @throws IllegalStateException if editing components on an interaction that has not been replied to yet
     */
    suspend fun editComponents(components: List<LayoutComponent>)


    /////////////
    /// MODAL ///
    /////////////

    /**
     * @throws IllegalStateException if the interaction doesn't support modal replies
     */
    suspend fun replyModal(modal: Modal)


    ///////////////
    /// PREMIUM ///
    ///////////////

    /**
     * @throws IllegalStateException if the interaction doesn't support premium required replies
     */
    suspend fun replyPremiumRequired()


    ////////////////////
    /// PREDASHBOARD ///
    ////////////////////
    /// The following functions are not good practice, try to not use them if possible ///
    ////////////////////

    fun setTimeoutAmount(timeoutAmount: Long)

    suspend fun replyWithConfirmation(
        description: String,
        isDangerous: Boolean = true,
        onConfirmation: suspend () -> Unit
    )

    suspend fun replyWithButtons(
        embed: MessageEmbed,
        buttons: List<Button>,
        onClick: (suspend (buttonId: String) -> Unit)? = null
    )

    suspend fun replyWithOptionalButtons(
        embed: MessageEmbed,
        buttons: List<Button>,
        onClick: suspend (buttonId: String?) -> Unit
    )

    suspend fun replyWithSelectMenu(
        embed: MessageEmbed,
        selectMenu: StringSelectMenu,
        withCancelButton: Boolean = false,
        onSelect: suspend (values: List<String>) -> Unit
    )

    suspend fun replyWithModel(
        modal: Modal,
        onFill: suspend (event: ModalInteractionEvent) -> Unit
    )
}