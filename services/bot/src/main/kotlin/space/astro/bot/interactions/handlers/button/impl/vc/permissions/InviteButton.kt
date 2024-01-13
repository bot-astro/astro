package space.astro.bot.interactions.handlers.button.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_INVITE,
    action = InteractionAction.VC_INVITE
)
class InviteButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
): space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext,
    ) {
        val memberSelectMenu = interactionComponentBuilder.entitySelectMenu(
            id = InteractionIds.Menu.VC_BAN,
            placeholder = "Choose the users to invite in your voice channel",
            entityTypes = listOf(EntitySelectMenu.SelectTarget.USER),
            rangeMin = 1,
            rangeMax = 3
        )

        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.default("Choose the users to invite in your voice channel with the menu below"),
            component = memberSelectMenu
        )
    }
}