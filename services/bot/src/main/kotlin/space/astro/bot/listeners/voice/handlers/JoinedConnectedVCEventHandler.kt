package space.astro.bot.listeners.voice.handlers

import space.astro.bot.managers.roles.SimpleMemberRolesManager
import space.astro.bot.managers.vc.VCEvent
import space.astro.shared.core.models.database.ConnectionAction

fun VCEventHandler.handleJoinedConnectedVCEvent(
    event: VCEvent.JoinedConnectedVC,
    memberRolesManager: SimpleMemberRolesManager,
) {
    val role = event.vcEventData.guild.getRoleById(event.connectionData.roleID) ?: return

    when (event.connectionData.action) {
        ConnectionAction.ASSIGN -> memberRolesManager.add(role)
        ConnectionAction.REMOVE -> memberRolesManager.remove(role)
        ConnectionAction.TOGGLE -> {
            if (event.vcEventData.member.roles.any { it.id == role.id }) {
                memberRolesManager.remove(role)
            } else {
                memberRolesManager.add(role)
            }
        }
    }
}