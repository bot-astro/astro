package space.astro.bot.events.listeners.voice.handlers

import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.shared.core.models.database.ConnectionAction

fun VCEventHandler.handleLeftConnectedVCEvent(
    event: VCEvent.LeftConnectedVC,
    memberRolesManager: SimpleMemberRolesManager,
) {
    val role = event.vcEventData.guild.getRoleById(event.connectionData.roleID) ?: return

    when (event.connectionData.action) {
        ConnectionAction.ASSIGN -> memberRolesManager.remove(role)
        ConnectionAction.REMOVE -> memberRolesManager.add(role)
        ConnectionAction.TOGGLE -> {
            if (event.vcEventData.member.roles.any { it.id == role.id }) {
                memberRolesManager.remove(role)
            } else {
                memberRolesManager.add(role)
            }
        }
    }
}