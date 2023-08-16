package space.astro.bot.command

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel

data class CommandContext(
    val commandHandler: CommandHandler,
    val guild: Guild?,
    val member: Member?,
    val user: User,
    val channel: Channel
) {
    fun getMemberIdLong(): Long = member?.idLong ?: user.idLong
    fun getChannelIdLong(): Long = channel.idLong
    fun isFromGuild(): Boolean = guild != null
}