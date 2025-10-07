package space.astro.bot.interactions

import net.dv8tion.jda.api.Permission

/**
 * Generic Astro action
 *
 * @property premium
 * @property cooldown in milliseconds
 * @property botPermissions
 */
enum class InteractionAction(
    val premium: Boolean,
    val cooldown: Long,
    val botPermissions: List<Permission>,
    val memberPermissions: List<Permission>,
    val voteRequired: Boolean
) {
    GENERIC(false, 0, emptyList(), emptyList(), false),

    VC_CHAT(true, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), true),
    VC_LOGS(false, 2000, emptyList(), emptyList(), false),
    VC_CLAIM(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), true),
    VC_TRANSFER(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), true),
    VC_BAN(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_HIDE(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_INVITE(true, 5000, listOf(Permission.ADMINISTRATOR), emptyList(), true),
    VC_LOCK(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_PERMIT(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_UNHIDE(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_UNLOCK(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_BITRATE(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_LIMIT(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_NAME(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_REGION(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), true),
    VC_TEMPLATE(false, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), false),
    VC_WAITING_ROOM(true, 2000, listOf(Permission.ADMINISTRATOR), emptyList(), true),

    // PREDASHBOARD
    SETTINGS(false, 2000, listOf(Permission.ADMINISTRATOR), listOf(Permission.MANAGE_CHANNEL), false),
    HIGH_COOLDOWN_NO_ADMIN(false, 5000, listOf(), listOf(Permission.MANAGE_CHANNEL), false)
}