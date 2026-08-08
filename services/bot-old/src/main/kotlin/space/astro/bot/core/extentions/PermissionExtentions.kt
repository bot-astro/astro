package space.astro.bot.core.extentions

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.IPermissionHolder
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.managers.channel.attribute.IPermissionContainerManager
import net.dv8tion.jda.api.requests.restaction.ChannelAction
import space.astro.shared.core.models.database.ConfigurationErrorData

fun Long.toPermissionList() = Permission.getPermissions(this)

fun InsufficientPermissionException.toConfigurationErrorDto(guildId: String) = ConfigurationErrorData(
    guildId = guildId,
    description = "Astro is missing the ${permission.getName()} permission in channel with ID $channelId"
)

fun HierarchyException.toConfigurationErrorDto(guildId: String) = ConfigurationErrorData(
    guildId = guildId,
    description = "Astro is not high enough in the server settings roles hierarchy to assign or remove some roles."
)

/**
 * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
 * @throws java.lang.IllegalArgumentException
 *
 * @see [IPermissionContainerManager.putPermissionOverride] for exceptions
 */
fun<T: GuildChannel> ChannelAction<T>.modifyPermissionOverride(
    permissionOverride: PermissionOverride?,
    permissionHolder: IPermissionHolder,
    allow: List<Permission> = emptyList(),
    deny: List<Permission> = emptyList(),
): ChannelAction<T> = modifyPermissionOverride(
    permissionOverride = permissionOverride,
    permissionHolder = permissionHolder,
    allow = Permission.getRaw(allow),
    deny = Permission.getRaw(deny)
)

/**
 * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
 * @throws java.lang.IllegalArgumentException
 *
 * @see [ChannelAction.addPermissionOverride] for exceptions
 */
fun<T: GuildChannel> ChannelAction<T>.modifyPermissionOverride(
    permissionOverride: PermissionOverride?,
    permissionHolder: IPermissionHolder,
    allow: Long = 0L,
    deny: Long = 0L
): ChannelAction<T> {
    val updatedPermissionOverride = computeAllowedAndDeniedPermissionsFromPermissionOverride(
        permissionOverride = permissionOverride,
        allow = allow,
        deny = deny
    )

    return addPermissionOverride(
        permissionHolder,
        updatedPermissionOverride.first,
        updatedPermissionOverride.second
    )
}

/**
 * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
 * @throws java.lang.IllegalArgumentException
 *
 * @see [IPermissionContainerManager.putPermissionOverride] for exceptions
 */
fun<T: IPermissionContainer, M: IPermissionContainerManager<T, M>> IPermissionContainerManager<T, M>.modifyPermissionOverride(
    permissionHolder: IPermissionHolder,
    allow: Collection<Permission> = emptyList(),
    deny: Collection<Permission> = emptyList()
): M = modifyPermissionOverride(
    permissionHolder = permissionHolder,
    allow = Permission.getRaw(allow),
    deny = Permission.getRaw(deny)
)

/**
 * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
 * @throws java.lang.IllegalArgumentException
 *
 * @see [IPermissionContainerManager.putPermissionOverride] for exceptions
 */
fun<T: IPermissionContainer, M: IPermissionContainerManager<T, M>> IPermissionContainerManager<T, M>.modifyPermissionOverride(
    permissionHolder: IPermissionHolder,
    allow: Long = 0L,
    deny: Long = 0L
): M {
    val permissionOverride = this.channel.getPermissionOverride(permissionHolder)
    val updatedPermissionOverride = computeAllowedAndDeniedPermissionsFromPermissionOverride(
        permissionOverride = permissionOverride,
        allow = allow,
        deny = deny
    )

    return putPermissionOverride(
        permissionHolder,
        updatedPermissionOverride.first,
        updatedPermissionOverride.second
    )
}

/**
 * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException
 * @throws java.lang.IllegalArgumentException
 *
 * @see [IPermissionContainerManager.putPermissionOverride] for exceptions
 */
fun<T: IPermissionContainer, M: IPermissionContainerManager<T, M>> IPermissionContainerManager<T, M>.modifyMemberPermissionOverride(
    memberId: Long,
    allow: Long = 0L,
    deny: Long = 0L
): M {
    val permissionOverride = this.channel.memberPermissionOverrides.firstOrNull { it.idLong == memberId }
    val updatedPermissionOverride = computeAllowedAndDeniedPermissionsFromPermissionOverride(
        permissionOverride = permissionOverride,
        allow = allow,
        deny = deny
    )

    return putMemberPermissionOverride(
        memberId,
        updatedPermissionOverride.first,
        updatedPermissionOverride.second
    )
}

/**
 * Computes the raw allowed and denied permissions from a give [PermissionOverride] and the permissions to [allow] and [deny]
 *
 * @return a [Pair] containing the allowed permissions raw value ([Pair.first]) and the denied permissions raw value ([Pair.second])
 */
private fun computeAllowedAndDeniedPermissionsFromPermissionOverride(
    permissionOverride: PermissionOverride?,
    allow: Long,
    deny: Long
): Pair<Long, Long> {
    var allowedPermissions = permissionOverride?.allowedRaw ?: 0L
    var deniedPermission = permissionOverride?.deniedRaw ?: 0L

    allowedPermissions = allowedPermissions or allow
    deniedPermission = deniedPermission and allow.inv()

    deniedPermission = deniedPermission or deny
    allowedPermissions = allowedPermissions and deny.inv()

    return allowedPermissions to deniedPermission
}