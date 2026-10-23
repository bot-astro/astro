package space.astro.bot.commands.vc.settings

import net.dv8tion.jda.api.Region
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.StringSelectHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Embeds
import space.astro.bot.ui.Emojis

@Component
class RegionMenu(
    contextResolverFactory: VcContextResolverFactory,
) : StringSelectHandler<VcInteractionContext> {
    companion object {
        const val MENU_ID = "menu/vc/region"
    }

    override val id = MENU_ID
    override val settings = InteractionSettings(ack = Ack.DEFER_EDIT)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: StringSelectInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val region = Region.fromKey(event.values.firstOrNull() ?: "")

        if (region == Region.UNKNOWN) {
            ctx.responder.error("Unknown region")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        if (vcCtx.temporaryVC.region.key != region.key) {
            vcCtx.temporaryVCManager.setRegion(region)
            vcCtx.markTemporaryVCManagerAsUpdated()
            vcCtx.queueUpdatedManagers()
        }

        ctx.responder.reply(Embeds.default("${Emojis.region.formatted} Region set to ${region.emoji?.plus(" ") ?: ""}${region.getName()}"))
    }
}
