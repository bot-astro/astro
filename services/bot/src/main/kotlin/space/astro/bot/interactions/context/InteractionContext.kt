package space.astro.bot.interactions.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import space.astro.bot.interactions.InteractionResponder
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

open class InteractionContext(
    val guild: Guild,
    val member: Member,
    val guildSettings: GuildSettingsEntity?,
    val responder: InteractionResponder,
) {
    val guildId: String = guild.id
    val memberId: String = member.id
}

class VcInteractionContext(
    val vcOperationCTX: VCOperationCTX,
    base: InteractionContext,
) : InteractionContext(base.guild, base.member, base.guildSettings, base.responder)
