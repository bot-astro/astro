package space.astro.bot.interactions.button.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.button.AbstractButton
import space.astro.bot.interactions.button.Button
import space.astro.bot.interactions.button.ButtonRunnable
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_PERMIT,
    action = InteractionAction.VC_PERMIT
)
class PermitButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
) : AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext
    ) {
        val memberAndRoleSelectMenu = interactionComponentBuilder.entitySelectMenu(
            id = InteractionIds.Menu.VC_BAN,
            placeholder = "Choose the users and roles to permit in your voice channel",
            entityTypes = listOf(EntitySelectMenu.SelectTarget.USER, EntitySelectMenu.SelectTarget.ROLE),
            rangeMin = 1,
            rangeMax = 10
        )

        event.replyEmbeds(Embeds.default("Choose the users and roles to permit in your voice channel via the menu below"))
            .setActionRow(memberAndRoleSelectMenu)
            .queue()
    }
}