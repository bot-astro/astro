package space.astro.bot.interactions.handlers.button.impl.vc.ownership

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable

import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_TRANSFER,
    action = InteractionAction.VC_TRANSFER
)
class TransferButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext
    ) {
        val members = ctx.vcOperationCTX.temporaryVC.members.filter { !it.user.isBot && it.id != ctx.memberId }

        if (members.isEmpty()) {
            ctx.replyHandler.replyEmbed(Embeds.error("Cannot find a user in your voice channel that is not a bot.\nRun this command again when someone joins your voice channel"))
            return
        }

        val memberSelectMenu = interactionComponentBuilder.selectMenu(
            id = InteractionIds.Menu.VC_TRANSFER,
            placeholder = "Choose the new owner of your voice channel",
            options = members.take(25).map {
                SelectOption.of(it.effectiveName, it.id)
                    .withDescription(it.user.name)
                    .withEmoji(Emojis.user)
            }
        )

        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.default("Choose the new owner of your voice channel with the menu below"),
            component = memberSelectMenu
        )
    }
}