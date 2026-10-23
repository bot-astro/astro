package space.astro.bot.events.listeners.discord.voice.handlers

import net.dv8tion.jda.api.entities.Role
import org.springframework.stereotype.Component
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.shared.core.models.database.guildSettings.ConnectionData.ConnectionAction

@Component
class ConnectedVCHandler {
    fun handleJoined(
        event: VCEvent.JoinedConnectedVC,
        memberRolesManager: SimpleMemberRolesManager,
    ) {
        // TODO: premium, the legacy code checked the maximum amount of connections here
        val role = event.vcEventData.guild.getRoleById(event.connectionData.roleID) ?: return

        when (event.connectionData.action) {
            ConnectionAction.ASSIGN -> memberRolesManager.add(role)
            ConnectionAction.REMOVE -> memberRolesManager.remove(role)
            ConnectionAction.TOGGLE -> toggle(event, role, memberRolesManager)
        }
    }

    fun handleLeft(
        event: VCEvent.LeftConnectedVC,
        memberRolesManager: SimpleMemberRolesManager,
    ) {
        val role = event.vcEventData.guild.getRoleById(event.connectionData.roleID) ?: return

        when (event.connectionData.action) {
            ConnectionAction.ASSIGN -> memberRolesManager.remove(role)
            ConnectionAction.REMOVE -> memberRolesManager.add(role)
            ConnectionAction.TOGGLE -> toggle(event, role, memberRolesManager)
        }
    }

    private fun toggle(
        event: VCEvent,
        role: Role,
        memberRolesManager: SimpleMemberRolesManager,
    ) {
        if (event.vcEventData.member.roles.any { it.id == role.id }) {
            memberRolesManager.remove(role)
        } else {
            memberRolesManager.add(role)
        }
    }
}
