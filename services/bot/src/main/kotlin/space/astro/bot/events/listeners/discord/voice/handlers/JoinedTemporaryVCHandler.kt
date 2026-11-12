package space.astro.bot.events.listeners.discord.voice.handlers

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import org.springframework.stereotype.Component
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.bot.utils.extensions.modifyPermissionOverride
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.lang.L

@Component
class JoinedTemporaryVCHandler {
    fun handle(event: VCEvent.JoinedTemporaryVC) {
        val data = event.vcEventData
        val guild = data.guild
        val member = data.member
        val entity = event.temporaryVCEntity
        val reason = L.t(data.guildSettings.supportedLocale, "reason.vc.member_joined")

        val temporaryVC =
            data.joinedChannel?.asVoiceChannel()
                ?: throw IllegalStateException("Received joined temporary VC event with a null joined channel")

        // required for letting the user use the voice channel fully even when locked or hidden
        // see https://support.discord.com/hc/en-us/community/posts/16947316767127-Video-and-Send-messages-permissions-suppressed-by-Connect
        temporaryVC.manager
            .modifyPermissionOverride(
                permissionHolder = member,
                allow = PermissionSets.userTemporaryVCPermissions,
            ).reason(reason)
            .queue()

        // allow user to see the private text chat of the temporary VC if existing
        val privateChat =
            entity.chatID
                ?.let { guild.getTextChannelById(it) }
                ?.also {
                    it
                        .upsertPermissionOverride(member)
                        .grant(PermissionSets.userTemporaryVCChatPermissions)
                        .reason(reason)
                        .queue()
                }

        if (entity.chatLogs) {
            val logChat: GuildMessageChannel = privateChat ?: temporaryVC
            logChat.sendVCLog(data.guildSettings.supportedLocale, "vc.log.joined", "user" to member.asMention)
        }
    }
}
