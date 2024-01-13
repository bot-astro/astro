package space.astro.bot.interactions.handlers.menu.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import space.astro.bot.components.managers.vc.VCPermissionManager
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
    id = InteractionIds.Menu.VC_BAN,
    action = InteractionAction.VC_BAN
)
class BanMenu(
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
        val users = event.values.mapNotNull { ctx.guild.getMemberById(it.id) }
        val roles = event.values.mapNotNull { ctx.guild.getRoleById(it.id) }

        val banned = vcPermissionManager.kickAndBanMultipleMembersAndRoles(
            vcOperationCTX = ctx.vcOperationCTX,
            users = users,
            roles = roles
        )

        event.hook.editOriginalEmbeds(
            Embeds.default(
            "The following users and roles have been banned from your voice channel:" +
                    "\n${banned.joinToString(", ") { it.asMention }}"
        )).queue()
    }
}