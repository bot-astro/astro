package space.astro.bot.components.managers.vc

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.interactions.InteractionException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.utils.extensions.modifyMemberPermissionOverride
import space.astro.bot.utils.extensions.modifyPermissionOverride
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.TemporaryVCEntity.TemporaryVCState
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

/**
 * All the methods only modify the managers of the [VCOperationCTX] and mark them as updated,
 * the changes are applied once [VCOperationCTX.queueUpdatedManagers] is called
 */
@Component
class VCPermissionManager(
    private val temporaryVCRepository: TemporaryVCRepository,
) {
    /**
     * Changes the state of the temporary vc and saves the entity
     *
     * TODO: refresh the vc name on state change (see TODO in [VCNameManager.performVCRename])
     *
     * @throws InsufficientPermissionException
     * @throws ConfigurationException if the target role of the generator does not exist anymore
     */
    suspend fun changeState(
        ctx: VCOperationCTX,
        newState: TemporaryVCState,
    ) {
        val entity = ctx.temporaryVCEntity

        if (newState == entity.state) {
            return
        }

        val targetRoleId = ctx.generatorSettings.permissionsTargetRole
        val targetRole =
            if (targetRoleId == null) {
                ctx.guild.publicRole
            } else {
                ctx.guild.getRoleById(targetRoleId)
                    ?: throw ConfigurationException(
                        ConfigurationErrorEntity(
                            guildId = ctx.guild.id,
                            description =
                                "The generator ${ctx.generator.name} has a target role set " +
                                    "but Astro couldn't find that role in your server!",
                            guide = ConfigurationErrorEntity.Guide.GENERATOR,
                        ),
                    )
            }

        val statePermissions = GeneratorSettings.VCState.valueOf(newState.name)
        entity.state = newState

        statePermissions.permissionDenied?.let { denied ->
            ctx.temporaryVCManager.modifyPermissionOverride(targetRole, 0L, denied.rawValue)
        }

        statePermissions.permissionReset?.let { reset ->
            resetPermission(ctx, targetRole, reset)
        }

        ctx.markTemporaryVCManagerAsUpdated()

        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guild.id, entity) }
    }

    /**
     * Gives the members with the [memberIds] and the [roles] [Permission.VIEW_CHANNEL] and [Permission.VOICE_CONNECT]
     */
    fun permit(
        ctx: VCOperationCTX,
        memberIds: List<Long>,
        roles: List<Role>,
    ) {
        val permitted = Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)

        memberIds.forEach { ctx.temporaryVCManager.modifyMemberPermissionOverride(it, permitted, 0L) }
        roles.forEach { ctx.temporaryVCManager.modifyPermissionOverride(it, permitted, 0L) }

        ctx.markTemporaryVCManagerAsUpdated()
    }

    /**
     * Gives the [members] [Permission.VIEW_CHANNEL] and [Permission.VOICE_CONNECT]
     */
    fun permitMembers(
        ctx: VCOperationCTX,
        members: List<Member>,
    ) {
        val permitted = Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)

        members.forEach { ctx.temporaryVCManager.modifyPermissionOverride(it, permitted, 0L) }

        ctx.markTemporaryVCManagerAsUpdated()
    }

    /**
     * Kicks a member from the vc (if present) and denies [Permission.VOICE_CONNECT]
     *
     * @throws InteractionException if the member has the immune role
     */
    suspend fun kickAndBanMember(
        ctx: VCOperationCTX,
        member: Member,
    ) {
        if (isImmune(ctx, member)) {
            throw InteractionException("${member.asMention} has a role which is immune to this command!")
        }

        if (member.voiceState?.channel?.id == ctx.temporaryVC.id) {
            ctx.guild.kickVoiceMember(member).await()
            delay(450)
        }

        ctx.temporaryVCManager.modifyPermissionOverride(member, 0L, Permission.VOICE_CONNECT.rawValue)
        ctx.markTemporaryVCManagerAsUpdated()
    }

    /**
     * Denies [Permission.VOICE_CONNECT] to the role
     *
     * @throws InteractionException if the role is the immune role
     */
    fun banRole(
        ctx: VCOperationCTX,
        role: Role,
    ) {
        if (ctx.generatorSettings.permissionsImmuneRole == role.id) {
            throw InteractionException("The ${role.asMention} role is immune to this command!")
        }

        ctx.temporaryVCManager.modifyPermissionOverride(role, 0L, Permission.VOICE_CONNECT.rawValue)
        ctx.markTemporaryVCManagerAsUpdated()
    }

    /**
     * Kicks the [members] from the vc and denies [Permission.VOICE_CONNECT] to [members] and [roles],
     * skipping the ones that are immune
     *
     * @return the effectively banned members and roles
     */
    suspend fun kickAndBanMembersAndRoles(
        ctx: VCOperationCTX,
        members: List<Member>,
        roles: List<Role>,
    ): List<IMentionable> {
        val membersToKick = mutableListOf<Member>()
        val banned = mutableListOf<IMentionable>()
        val immuneRoleId = ctx.generatorSettings.permissionsImmuneRole

        members.filterNot { isImmune(ctx, it) }.forEach { member ->
            banned.add(member)
            ctx.temporaryVCManager.modifyPermissionOverride(member, 0L, Permission.VOICE_CONNECT.rawValue)

            if (member.voiceState?.channel?.id == ctx.temporaryVC.id) {
                membersToKick.add(member)
            }
        }

        roles.filter { it.id != immuneRoleId }.forEach { role ->
            banned.add(role)
            ctx.temporaryVCManager.modifyPermissionOverride(role, 0L, Permission.VOICE_CONNECT.rawValue)
        }

        // Those delays are to prevent spam and because when a user exits a temp vc its permissions get removed,
        // but we also need to explicitly deny some other perms after
        membersToKick.forEach {
            ctx.guild.kickVoiceMember(it).await()
            delay(250)
        }
        delay(200)

        ctx.markTemporaryVCManagerAsUpdated()

        return banned
    }

    private fun isImmune(
        ctx: VCOperationCTX,
        member: Member,
    ): Boolean {
        val immuneRoleId = ctx.generatorSettings.permissionsImmuneRole ?: return false

        return member.roles.any { it.id == immuneRoleId }
    }

    /**
     * Restores [permission] for [targetRole] to what the generator originally had (allowed, denied or neutral)
     */
    private fun resetPermission(
        ctx: VCOperationCTX,
        targetRole: Role,
        permission: Permission,
    ) {
        val originalOverrides =
            when (ctx.generatorSettings.permissionsInherited) {
                GeneratorSettings.PermissionsInherited.NONE -> null
                GeneratorSettings.PermissionsInherited.GENERATOR -> ctx.generator.permissionOverrides
                GeneratorSettings.PermissionsInherited.CATEGORY -> ctx.generator.parentCategory?.permissionOverrides
            }
        val originalOverride = originalOverrides?.firstOrNull { it.id == targetRole.id }

        val wasAllowed = originalOverride?.allowed?.contains(permission) ?: false
        val wasDenied = !wasAllowed && (originalOverride?.denied?.contains(permission) ?: false)

        val currentOverride = ctx.temporaryVC.permissionOverrides.firstOrNull { it.id == targetRole.id } ?: return

        when {
            wasAllowed -> ctx.temporaryVCManager.modifyPermissionOverride(targetRole, permission.rawValue, 0L)
            wasDenied -> ctx.temporaryVCManager.modifyPermissionOverride(targetRole, 0L, permission.rawValue)
            else ->
                ctx.temporaryVCManager.putPermissionOverride(
                    targetRole,
                    currentOverride.allowed.apply { remove(permission) },
                    currentOverride.denied.apply { remove(permission) },
                )
        }
    }
}
