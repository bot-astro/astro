package space.astro.bot.managers.roles

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import java.lang.Exception

/**
 * Simple member roles manager that allows to set roles to add and to remove from a member
 */
class SimpleMemberRolesManager(
    private val guild: Guild,
    private val member: Member
) {
    private val rolesToAdd = mutableListOf<Role>()
    private val rolesToRemove = mutableListOf<Role>()

    fun add(role: Role) {
        if (rolesToAdd.none { it.id == role.id }) {
            rolesToAdd.add(role)
        }
    }

    fun remove(role: Role) {
        if (rolesToRemove.none { it.id == role.id }) {
            rolesToRemove.add(role)
        }
    }

    /**
     * Queues the role updates
     *
     * **See [Guild.modifyMemberRoles] for the possible exceptions**
     */
    fun queue() {
        if (rolesToAdd.isNotEmpty() || rolesToRemove.isNotEmpty()) {
            rolesToRemove.removeAll { it.id in rolesToAdd.map { it.id } }

            try {
                guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue()
            } catch (e: Exception) {
                throw e
            }
        }
    }
}