package space.astro.bot.components.managers.vc

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.exceptions.VcOperationException
import space.astro.bot.core.extentions.modifyMemberPermissionOverride
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
     * Remember to save the new temporaryVCData, its state property gets changed when calling this function
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
            // Get the original permissions inherited for the temporary vc
            val originalPermissionOverrides = when (vcOperationCTX.generatorData.permissionsInherited) {
                PermissionsInherited.NONE -> null
                PermissionsInherited.GENERATOR -> vcOperationCTX.generator.permissionOverrides
                PermissionsInherited.CATEGORY -> vcOperationCTX.generator.parentCategory?.permissionOverrides
            }
            val originalPermissionOverride = originalPermissionOverrides?.firstOrNull { it.id == targetRole.id }

            // Find whether the permission to reset was originally allowed or denied
            val isOriginalPermissionAllowed = originalPermissionOverride?.allowed?.contains(newState.permissionReset) ?: false
            val isOriginalPermissionDenied = if (isOriginalPermissionAllowed) false else originalPermissionOverride?.denied?.contains(newState.permissionReset) ?: false

            // Get the permission override for the target role
            val permissionOverrideInheritedForTargetRole = vcOperationCTX.temporaryVC.permissionOverrides.firstOrNull { it.id == targetRole.id }

            if (permissionOverrideInheritedForTargetRole != null) {
                // If the original was allowed, allow it for the target role
                // If it was denied, deny it
                // If it was neither, set it to neutral
                if (isOriginalPermissionAllowed) {
                    vcOperationCTX.temporaryVCManager.modifyPermissionOverride(targetRole, newState.permissionReset?.rawValue ?: 0L, 0L)
                } else if (isOriginalPermissionDenied) {
                    vcOperationCTX.temporaryVCManager.modifyPermissionOverride(targetRole, 0L, newState.permissionReset?.rawValue ?: 0L)
                } else {
                    val allowed = permissionOverrideInheritedForTargetRole.allowed.apply { remove(newState.permissionReset) }
                    val denied = permissionOverrideInheritedForTargetRole.denied.apply { remove(newState.permissionReset) }

                    vcOperationCTX.temporaryVCManager.putPermissionOverride(
                        targetRole,
                        allowed,
                        denied
                    )
                }
            }
        }

        vcOperationCTX.markTemporaryVCManagerAsUpdated()

        try {
            vcNameManager.performVCNameRefresh(vcOperationCTX)
        } catch (_: VcOperationException) {}
    }

    /**
     * Gives all members with a specific id and [roles] [Permission.VIEW_CHANNEL] and [Permission.VOICE_CONNECT]
     */
    fun permit(
        vcOperationCTX: VCOperationCTX,
        memberIds: List<Long>,
        roles: List<Role>
    ) {
        memberIds.forEach { memberId ->
            vcOperationCTX.temporaryVCManager.modifyMemberPermissionOverride(
                memberId,
                Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
                0
            )
        }

        roles.forEach { entity ->
            vcOperationCTX.temporaryVCManager.modifyPermissionOverride(
                entity,
                Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
                0
            )
        }

        vcOperationCTX.temporaryVCManager.queue()
    }

    /**
     * Kicks a member from a VC and denies [Permission.VOICE_CONNECT]
     *
     * @throws VcOperationException
     */
    suspend fun kickAndBanMember(
        vcOperationCTX: VCOperationCTX,
        member: Member
    ) {
        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        if (immuneRoleId != null && member.roles.any { it.id == immuneRoleId }) {
            throw VcOperationException(VcOperationException.Reason.CANNOT_BAN_IMMUNE_ROLE)
        }

        if (member.voiceState!!.channel?.id == vcOperationCTX.temporaryVC.id) {
            vcOperationCTX.guild.kickVoiceMember(member).await()
            delay(450)
        }

        vcOperationCTX.temporaryVC.manager.putPermissionOverride(
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
    suspend fun kickAndBanMultipleMembers(
        vcOperationCTX: VCOperationCTX,
        members: List<Member>
    ): List<Member> {
        val membersToKick = mutableListOf<Member>()
        val banned = mutableListOf<Member>()
        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        members.forEach { member ->
            if (member.roles.none { it.id == immuneRoleId }) {
                banned.add(member)

                vcOperationCTX.temporaryVC.manager.putPermissionOverride(
                    member,
                    0,
                    Permission.VOICE_CONNECT.rawValue
                )
            }

            if (member.voiceState!!.channel?.id == vcOperationCTX.temporaryVC.id) {
                membersToKick.add(member)
            }
        }

        // Those delays are to prevent spam and because when a user exists a temp vc its permissions get removed
        // But we also need to explicitly deny some other perms after
        membersToKick.forEach {
            vcOperationCTX.guild.kickVoiceMember(it).await()
            delay(250)
        }
        delay(200)

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
    suspend fun kickAndBanMultipleMembersAndRoles(
        vcOperationCTX: VCOperationCTX,
        members: List<Member>,
        roles: List<Role>
    ): List<IMentionable> {
        val membersToKick = mutableListOf<Member>()
        val banned = mutableListOf<IMentionable>()
        val immuneRoleId = vcOperationCTX.generatorData.permissionsImmuneRole

        members.forEach { member ->
            if (member.roles.none { it.id == immuneRoleId }) {
                banned.add(member)

                vcOperationCTX.temporaryVCManager.putPermissionOverride(
                    member,
                    0,
                    Permission.VOICE_CONNECT.rawValue
                )
            }

            if (member.voiceState!!.channel?.id == vcOperationCTX.temporaryVC.id) {
                membersToKick.add(member)
            }
        }

        roles.forEach { role ->
            if (immuneRoleId != role.id) {
                banned.add(role)
                vcOperationCTX.temporaryVCManager.modifyPermissionOverride(
                    role,
                    0,
                    Permission.VOICE_CONNECT.rawValue
                )
            }
        }

        // Those delays are to prevent spam and because when a user exists a temp vc its permissions get removed
        // But we also need to explicitly deny some other perms after
        membersToKick.forEach {
            vcOperationCTX.guild.kickVoiceMember(it).await()
            delay(250)
        }
        delay(200)

        vcOperationCTX.temporaryVCManager.queue()

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