package space.astro.bot.interactions.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import space.astro.bot.interactions.InteractionResponder
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import java.util.Locale

open class InteractionContext(
    val guild: Guild,
    val member: Member,
    val guildSettings: GuildSettingsEntity?,
    val responder: InteractionResponder,
) {
    val guildId: String = guild.id
    val memberId: String = member.id

    /** locale of the user that triggered the interaction */
    val locale: Locale = responder.locale

    fun t(
        key: String,
        vararg args: Pair<String, Any?>,
    ): String = L.t(locale, key, *args)
}

class VcInteractionContext(
    val vcOperationCTX: VCOperationCTX,
    base: InteractionContext,
) : InteractionContext(base.guild, base.member, base.guildSettings, base.responder)
