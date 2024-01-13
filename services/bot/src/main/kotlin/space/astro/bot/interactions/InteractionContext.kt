package space.astro.bot.interactions

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.models.database.*

open class InteractionContext(
    val guild: Guild,
    val member: Member,
    val interactionReplyManager: InteractionReplyManager
) {
    val user = member.user
    val guildId = guild.id
    val memberId = member.id
}

class VcInteractionContext(
    val vcOperationCTX: VCOperationCTX,
    guild: Guild,
    member: Member,
    interactionReplyManager: InteractionReplyManager
) : InteractionContext(guild, member, interactionReplyManager)

class SettingsInteractionContext(
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    interactionReplyManager: InteractionReplyManager
) : InteractionContext(guild, member, interactionReplyManager)

class GeneratorSettingsInteractionContext(
    val generatorData: GeneratorData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    interactionReplyManager: InteractionReplyManager
) : InteractionContext(guild, member, interactionReplyManager)

class InterfaceSettingsInteractionContext(
    val interfaceData: InterfaceData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    interactionReplyManager: InteractionReplyManager
) : InteractionContext(guild, member, interactionReplyManager)

class ConnectionSettingsInteractionContext(
    val connectionData: ConnectionData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    interactionReplyManager: InteractionReplyManager
) : InteractionContext(guild, member, interactionReplyManager)

class TemplateSettingsInteractionContext(
    val templateData: TemplateData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    interactionReplyManager: InteractionReplyManager
) : InteractionContext(guild, member, interactionReplyManager)