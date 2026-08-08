package space.astro.bot.models.discord.vc.event

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import space.astro.shared.core.models.database.GuildData
import space.astro.shared.core.models.database.TemporaryVCData

/**
 * @param event the voice update event that triggered this [VCEventData]
 * @param guildData
 * @param temporaryVCs list of existing temporary VCs
 */
data class VCEventData(
    val event: GuildVoiceUpdateEvent,
    val guildData: GuildData,
    val temporaryVCs: List<TemporaryVCData>,
) {
    val guild = event.guild
    val member = event.member
    val userId = event.member.id
    val joinedChannel = event.channelJoined
    val leftChannel = event.channelLeft

    val generators = guildData.generators
    val connections = guildData.connections
}