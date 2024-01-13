package space.astro.bot.interactions.menu.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.interactions.menu.AbstractMenu
import space.astro.bot.interactions.menu.Menu
import space.astro.bot.interactions.menu.MenuRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Menu(
    id = InteractionIds.Menu.VC_BAN,
    action = InteractionAction.VC_BAN
)
class PermitMenu(
    private val vcPermissionManager: VCPermissionManager
) : AbstractMenu() {
    @MenuRunnable
    fun run(
        event: EntitySelectInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        val entities = event.values.mapNotNull {
            ctx.guild.getMemberById(it.id) ?: ctx.guild.getRoleById(it.id)
        }

        vcPermissionManager.permit(
            vcOperationCTX = ctx.vcOperationCTX,
            entities = entities
        )

        event.hook.editOriginalEmbeds(
            Embeds.default(
            "The following users and roles have been permitted in your voice channel:" +
                    "\n${entities.joinToString(", ") { it.asMention }}"
        )).queue()
    }
}