package space.astro.bot.command

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import space.astro.bot.models.discord.vc.VCOperationCTX

open class CommandContext(
    val commandHandler: CommandHandler,
    val guild: Guild,
    val member: Member,
    val user: User,
    val channel: Channel
) {
    val guildId = guild.id
    val memberId = member.id
}

class VcCommandContext(
    val vcOperationCTX: VCOperationCTX,
    commandHandler: CommandHandler,
    guild: Guild,
    member: Member,
    user: User,
    channel: Channel
) : CommandContext(commandHandler, guild, member, user, channel)