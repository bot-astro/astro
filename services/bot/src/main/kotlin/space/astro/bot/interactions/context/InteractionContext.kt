package space.astro.bot.interactions.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import space.astro.bot.interactions.reply.IInteractionReplyHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.models.database.*

open class InteractionContext(
    val guild: Guild,
    val member: Member,
    val replyHandler: IInteractionReplyHandler
) {
    val user = member.user
    val guildId = guild.id
    val memberId = member.id
}

class VcInteractionContext(
    val vcOperationCTX: VCOperationCTX,
    guild: Guild,
    member: Member,
    replyHandler: IInteractionReplyHandler
) : InteractionContext(guild, member, replyHandler)

class SettingsInteractionContext(
    val guildData: GuildData,
    val userData: UserData,
    guild: Guild,
    member: Member,
    replyHandler: IInteractionReplyHandler
) : InteractionContext(guild, member, replyHandler)

class GeneratorSettingsInteractionContext(
    val generatorData: GeneratorData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    replyHandler: IInteractionReplyHandler
) : InteractionContext(guild, member, replyHandler) {
    val generatorIndex = guildData.generators.indexOfFirst { it.id == generatorData.id }
}

class InterfaceSettingsInteractionContext(
    val interfaceData: InterfaceData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    replyHandler: IInteractionReplyHandler
) : InteractionContext(guild, member, replyHandler) {
    val interfaceIndex = guildData.interfaces.indexOfFirst { it.messageID == interfaceData.messageID }
}

class ConnectionSettingsInteractionContext(
    val connectionData: ConnectionData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    replyHandler: IInteractionReplyHandler
) : InteractionContext(guild, member, replyHandler) {
    val connectionIndex = guildData.connections.indexOfFirst { it.id == connectionData.id && it.roleID == connectionData.roleID }
}

class TemplateSettingsInteractionContext(
    val templateData: TemplateData,
    val guildData: GuildData,
    guild: Guild,
    member: Member,
    replyHandler: IInteractionReplyHandler
) : InteractionContext(guild, member, replyHandler) {
    val templateIndex = guildData.templates.indexOfFirst { it.id == templateData.id }
}