package space.astro.bot.interactions.button

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import space.astro.bot.models.discord.vc.VCOperationCTX

open class ButtonContext(
    val buttonHandler: ButtonHandler,
    val guild: Guild,
    val member: Member,
    val user: User,
    val channel: Channel
) {
    val guildId = guild.id
    val memberId = member.id
}

class VcButtonContext(
    val vcOperationCTX: VCOperationCTX,
    buttonHandler: ButtonHandler,
    guild: Guild,
    member: Member,
    user: User,
    channel: Channel
) : ButtonContext(buttonHandler, guild, member, user, channel)