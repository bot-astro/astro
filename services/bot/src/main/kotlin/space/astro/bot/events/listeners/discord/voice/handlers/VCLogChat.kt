package space.astro.bot.events.listeners.discord.voice.handlers

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.bot.utils.extensions.toPermissionList
import space.astro.shared.core.lang.L
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

    sendMessage(
        Containers.message(
            Containers.container(
                guild.selfMember.colorRaw,
                Containers.title(L.t(locale, "vc.log.title", "emoji" to Emojis.logs.formatted)),
                Containers.text(L.t(locale, key, *args)),
                Containers.footer(L.t(locale, "vc.log.footer")),
            ),
        ),
    ).queue()
}
