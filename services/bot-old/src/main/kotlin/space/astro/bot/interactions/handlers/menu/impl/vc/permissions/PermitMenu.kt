package space.astro.bot.interactions.handlers.menu.impl.vc.permissions

import net.dv8tion.jda.api.entities.Role
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
    id = InteractionIds.Menu.VC_PERMIT,
    action = InteractionAction.VC_PERMIT
)
class PermitMenu(
    private val vcPermissionManager: VCPermissionManager
) : AbstractMenu() {
    @MenuRunnable
    suspend fun run(
        event: EntitySelectInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        ctx.replyHandler.deferReply()
        val memberIds = mutableListOf<Long>()
        val roles = mutableListOf<Role>()

        event.values.mapNotNull {
            val role = ctx.guild.getRoleById(it.id)
            if (role != null) {
                roles.add(role)
            } else {
                memberIds.add(it.idLong)
            }
        }

        vcPermissionManager.permit(
            vcOperationCTX = ctx.vcOperationCTX,
            memberIds = memberIds,
            roles = roles
        )

        val response = StringBuilder()
        if (memberIds.isNotEmpty()) {
            response.append("Users permitted: ${memberIds.joinToString(", ") { "<@$it>" }}")
        }
        if (roles.isNotEmpty()) {
            response.append("Roles permitted: ${roles.joinToString(", ") { it.asMention }}")
        }
        if (memberIds.isEmpty() && roles.isEmpty()) {
            response.append("You didn't provide valid users / roles")
        }

        ctx.replyHandler.replyEmbed(
            Embeds.default(
            "The following users and roles have been permitted in your voice channel:" +
                    "\n${memberIds.joinToString(", ") { "<@$it>" }}"
        ))
    }
}