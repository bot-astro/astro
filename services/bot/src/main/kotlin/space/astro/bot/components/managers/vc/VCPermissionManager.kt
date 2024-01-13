package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.IPermissionHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.exceptions.VcOperationException
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.models.database.PermissionsInherited
import space.astro.shared.core.models.database.VCState

@Component
class VCPermissionManager(
    private val vcNameManager: VCNameManager,
    private val configurationErrorService: ConfigurationErrorService
) {
    /**
     * @throws InsufficientPermissionException
     * @throws ConfigurationException
     */
    fun changeState(
        vcOperationCTX: VCOperationCTX,
        newState: VCState
    ) {
        if (newState == vcOperationCTX.temporaryVCData.state) {
            return
        }

        val targetRole = if (vcOperationCTX.generatorData.permissionsTargetRole != null) {
            vcOperationCTX.guild.getRoleById(vcOperationCTX.generatorData.permissionsTargetRole!!)
                ?: throw ConfigurationException(configurationErrorService.missingGeneratorTargetRole(vcOperationCTX.generator.name))
        } else {
            vcOperationCTX.guild.publicRole
        }

        vcOperationCTX.temporaryVCData.state = newState

        if (newState.permissionDenied != null) {
            vcOperationCTX.temporaryVCManager.modifyPermissionOverride(
                targetRole,
                0,
                newState.permissionDenied!!.rawValue
            )
        }

        if (newState.permissionReset != null) {
            val permissionOverrideInheritedForTargetRole = vcOperationCTX.calculateInheritedPermissions()
                .firstOrNull { it.id == targetRole.id }

            if (permissionOverrideInheritedForTargetRole != null) {
                val allowed = permissionOverrideInheritedForTargetRole.allowed.apply { remove(newState.permissionReset) }
                val denied = permissionOverrideInheritedForTargetRole.denied.apply { remove(newState.permissionReset) }

                vcOperationCTX.temporaryVCManager.modifyPermissionOverride(
                    targetRole,
                    allowed,
                    denied
                )
            }
        }

        vcOperationCTX.markTemporaryVCManagerAsUpdated()

        try {
            vcNameManager.performVCNameRefresh(vcOperationCTX)
        } catch (_: VcOperationException) {}
    }

    /**
     * Gives all [entities] [Permission.VIEW_CHANNEL] and [Permission.VOICE_CONNECT]
     */
    fun permit(
        vcOperationCTX: VCOperationCTX,
        entities: List<IPermissionHolder>
    ) {
        entities.forEach { entity ->
            vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
                entity,
                Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
                0
            )
        }

        vcOperationCTX.temporaryVC.manager.queue()
    }

    /**
     * Kicks a member from a VC and denies [Permission.VOICE_CONNECT]
     *
     * @throws VcOperationException
     */
    fun kickAndBanMember(
        vcOperationCTX: VCOperationCTX,
        member: Member
    ) {
        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        if (immuneRoleId != null && member.roles.any { it.id == immuneRoleId }) {
            throw VcOperationException(VcOperationException.Reason.CANNOT_BAN_IMMUNE_ROLE)
        }

        if (member.voiceState!!.channel?.id == vcOperationCTX.temporaryVC.id) {
            vcOperationCTX.guild.kickVoiceMember(member).queue()
        }

        vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
            member,
            0,
            Permission.VOICE_CONNECT.rawValue
        ).queue()
    }

    /**
     * Kicks multiple members from a VC and denies [Permission.VOICE_CONNECT]
     *
     * @return the list of banned [Member]
     */
    fun kickAndBanMultipleMembers(
        vcOperationCTX: VCOperationCTX,
        members: List<Member>
    ): List<Member> {
        val banned = mutableListOf<Member>()
        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        members.forEach { member ->
            if (member.roles.none { it.id == immuneRoleId }) {
                banned.add(member)

                vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
                    member,
                    0,
                    Permission.VOICE_CONNECT.rawValue
                )
            }

            if (member.voiceState!!.channel?.id == vcOperationCTX.temporaryVC.id) {
                vcOperationCTX.guild.kickVoiceMember(member).queue()
            }
        }

        vcOperationCTX.temporaryVC.manager.queue()

        return banned
    }

    /**
     * Denies [Permission.VOICE_CONNECT] to the role
     *
     * @throws VcOperationException
     */
    fun banRole(
        vcOperationCTX: VCOperationCTX,
        role: Role
    ) {
        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        if (immuneRoleId == role.id) {
            throw VcOperationException(VcOperationException.Reason.CANNOT_BAN_IMMUNE_ROLE)
        }

        vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
            role,
            0,
            Permission.VOICE_CONNECT.rawValue
        ).queue()
    }

    /**
     * Denies [Permission.VOICE_CONNECT] to multiple roles
     *
     * @return the list of banned [Role]
     */
    fun banMultipleRoles(
        vcOperationCTX: VCOperationCTX,
        roles: List<Role>
    ): List<Role> {
        val banned = mutableListOf<Role>()

        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        roles.forEach { role ->
            if (immuneRoleId != role.id) {
                banned.add(role)
                vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
                    role,
                    0,
                    Permission.VOICE_CONNECT.rawValue
                )
            }
        }

        vcOperationCTX.temporaryVC.manager.queue()

        return banned
    }

    /**
     * Kicks multiple member from a VC and denies [Permission.VOICE_CONNECT] to the members and roles
     *
     * @return the list of effectively banned members and roles as [IMentionable]
     */
    fun kickAndBanMultipleMembersAndRoles(
        vcOperationCTX: VCOperationCTX,
        users: List<Member>,
        roles: List<Role>
    ): List<IMentionable> {
        val banned = mutableListOf<IMentionable>()
        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        users.forEach { user ->
            if (user.roles.none { it.id == immuneRoleId }) {
                banned.add(user)

                vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
                    user,
                    0,
                    Permission.VOICE_CONNECT.rawValue
                )
            }

            if (user.voiceState!!.channel?.id == vcOperationCTX.temporaryVC.id) {
                vcOperationCTX.guild.kickVoiceMember(user).queue()
            }
        }

        roles.forEach { role ->
            if (immuneRoleId != role.id) {
                banned.add(role)
                vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
                    role,
                    0,
                    Permission.VOICE_CONNECT.rawValue
                )
            }
        }

        vcOperationCTX.temporaryVC.manager.queue()

        return banned
    }

    private fun VCOperationCTX.calculateInheritedPermissions(): List<PermissionOverride> {
        return when (generatorData.permissionsInherited) {
            PermissionsInherited.NONE -> {
                emptyList()
            }
            PermissionsInherited.GENERATOR -> {
                generator.permissionOverrides
            }
            PermissionsInherited.CATEGORY -> {
                temporaryVC.parentCategory?.permissionOverrides ?: emptyList()
            }
        }
    }
}