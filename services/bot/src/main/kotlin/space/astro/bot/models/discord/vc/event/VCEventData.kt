package space.astro.bot.models.discord.vc.event

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

/**
 * @param event the voice update event that triggered this [VCEventData]
 * @param temporaryVCEntities list of existing temporary VCs
 */
data class VCEventData(
    val event: GuildVoiceUpdateEvent,
    val guildSettings: GuildSettingsEntity,
    val temporaryVCEntities: List<TemporaryVCEntity>,
) {
    val guild = event.guild
    val member = event.member
    val userId = event.member.id
    val joinedChannel = event.channelJoined
    val leftChannel = event.channelLeft

    val generators = guildSettings.generators
    val connections = guildSettings.connections
}
