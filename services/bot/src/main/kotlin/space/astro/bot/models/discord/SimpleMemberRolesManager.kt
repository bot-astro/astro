package space.astro.bot.models.discord

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role

/**
 * Simple member roles manager that allows to set roles to add and to remove from a member
 */
class SimpleMemberRolesManager(
    private val guild: Guild,
    private val member: Member,
) {
    private val rolesToAdd = mutableListOf<Role>()
    private val rolesToRemove = mutableListOf<Role>()

    fun add(role: Role) {
        if (role.isManaged) return

        if (rolesToAdd.none { it.id == role.id }) {
            rolesToAdd.add(role)
        }
    }

    fun remove(role: Role) {
        if (role.isManaged) return

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
        if (rolesToAdd.isEmpty() && rolesToRemove.isEmpty()) return

        val addedIds = rolesToAdd.map { it.id }.toSet()
        rolesToRemove.removeAll { it.id in addedIds }

        guild
            .modifyMemberRoles(member, rolesToAdd, rolesToRemove)
            .reason("applied the necessary roles based on the generator and connection features settings")
            .queue()
    }
}
