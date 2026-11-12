package space.astro.bot.events.listeners.discord.voice.handlers

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.ui.Messages
import space.astro.bot.utils.extensions.toPermissionList
import java.util.Locale

/**
 * Sends a log message about a member in the chat of a temporary VC, if Astro is allowed to
 */
fun GuildMessageChannel.sendVCLog(
    locale: Locale,
    key: String,
    vararg args: Pair<String, Any?>,
) {
    if (!guild.selfMember.hasPermission(this, PermissionSets.astroSendMessagePermissions.toPermissionList())) {
        return
    }

    sendMessage(Messages.vcLog(guild, locale, key, *args)).queue()
}
