package space.astro.bot.interactions

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import space.astro.bot.models.discord.vc.VCOperationCTX

open class InteractionContext(
    val guild: Guild,
    val member: Member,
    val user: User,
    val channel: Channel
) {
    val guildId = guild.id
    val memberId = member.id
}

class VcInteractionContext(
    val vcOperationCTX: VCOperationCTX,
    guild: Guild,
    member: Member,
    user: User,
    channel: Channel
) : InteractionContext(guild, member, user, channel)