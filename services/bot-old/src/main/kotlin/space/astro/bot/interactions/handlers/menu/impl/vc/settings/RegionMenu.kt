package space.astro.bot.interactions.handlers.menu.impl.vc.settings

import net.dv8tion.jda.api.Region
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.menu.AbstractMenu
import space.astro.bot.interactions.handlers.menu.Menu
import space.astro.bot.interactions.handlers.menu.MenuRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Menu(
    id = InteractionIds.Menu.VC_REGION,
    action = InteractionAction.VC_REGION
)
class RegionMenu : AbstractMenu() {

    @MenuRunnable
    suspend fun run(
        event: StringSelectInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        ctx.replyHandler.deferReply()

        val region = Region.fromKey(event.values.firstOrNull())
        if (region != Region.UNKNOWN && ctx.vcOperationCTX.temporaryVC.region.key != region.key) {
            ctx.vcOperationCTX.temporaryVCManager.setRegion(region).queue()
        }

        ctx.replyHandler.replyEmbed(Embeds.default("Region set to ${region.emoji?.plus(" ")}${region.name}"))
    }
}