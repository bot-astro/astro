package space.astro.bot.events.listeners.discord.voice.handlers

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.ui.Emojis
import space.astro.bot.utils.extensions.toPermissionList

/**
 * Sends a log message about a member in the chat of a temporary VC, if Astro is allowed to
 */
fun GuildMessageChannel.sendVCLog(description: String) {
    if (!guild.selfMember.hasPermission(this, PermissionSets.astroSendMessagePermissions.toPermissionList())) {
        return
    }

    // TODO: components v2, replace the embed (last step of the migration)
    sendMessageEmbeds(
        EmbedBuilder()
            .setColor(guild.selfMember.colorRaw)
            .setTitle("${Emojis.logs.formatted} Voice channel logs")
            .setDescription(description)
            .setFooter("You can disable these logs with `/chat logs`")
            .build(),
    ).useComponentsV2(false).queue()
}
