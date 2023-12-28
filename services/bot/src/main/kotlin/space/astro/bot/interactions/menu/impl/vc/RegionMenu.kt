package space.astro.bot.interactions.menu.impl.vc

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.interactions.menu.AbstractMenu
import space.astro.bot.interactions.menu.Menu
import space.astro.bot.interactions.menu.MenuRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Menu(id = InteractionIds.Menu.VC_REGION)
class RegionMenu : AbstractMenu() {

    @MenuRunnable
    fun run(
        event: StringSelectInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        event.hook.editOriginalEmbeds(Embeds.default("nice!"))
            .setComponents()
            .queue()
    }
}