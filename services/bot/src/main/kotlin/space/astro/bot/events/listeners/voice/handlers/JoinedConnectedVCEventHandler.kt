package space.astro.bot.events.listeners.voice.handlers

import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.shared.core.models.database.ConnectionAction

fun VCEventHandler.handleJoinedConnectedVCEvent(
    event: VCEvent.JoinedConnectedVC,
    memberRolesManager: SimpleMemberRolesManager,
) {
    //////////////////////////
    /// PREMIUM REQUISITES ///
    //////////////////////////
    if (premiumRequirementDetector.exceededMaximumConnectionsAmount(event.vcEventData.guildData)) {
        throw ConfigurationException(configurationErrorService.maximumAmountOfConnections())
    }

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