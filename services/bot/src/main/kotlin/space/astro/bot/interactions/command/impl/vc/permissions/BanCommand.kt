package space.astro.bot.interactions.command.impl.vc.permissions

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.core.exceptions.VcOperationException
import space.astro.bot.interactions.command.*
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "ban",
    description = "Ban a user or a role from joining your voice channel",
    category = CommandCategory.VC,
    premium = true
    // TODO: Higher cooldown
)
class BanCommand(
    val vcPermissionManager: VCPermissionManager
) : AbstractCommand() {
    @SubCommand(
        name = "user",
        description = "Ban a user from your VC (also kicks the user from your VC if present)"
    )
    suspend fun user(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext,
        @CommandOption(
            type = OptionType.USER,
            name = "user",
            description = "The user to ban from your channel"
        ) member: Member,
    ) {
        try {
            vcPermissionManager.kickAndBanUser(ctx.vcOperationCTX, member)

            event.replyEmbeds(Embeds.default(
                "${member.asMention} has been banned from your temporary VC!"
            )).setEphemeral(true).queue()
        } catch (e: VcOperationException) {
            event.replyEmbeds(Embeds.error(
                "${member.asMention} has a role which is immune to this command!"
            )).setEphemeral(true).queue()
        }
    }

    @SubCommand(
        name = "role",
        description = "Ban a role from your VC"
    )
    suspend fun role(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext,
        @CommandOption(
            type = OptionType.USER,
            name = "role",
            description = "The role to ban from your channel"
        ) role: Role,
    ) {
        try {
            vcPermissionManager.banRole(ctx.vcOperationCTX, role)

            event.replyEmbeds(Embeds.default("${role.asMention} role has been ${Emojis.ban.formatted} banned from your temporary VC!"))
                .setEphemeral(true).queue()
        } catch (e: VcOperationException) {
            event.replyEmbeds(Embeds.error("The ${role.asMention} role is immune to this command!"))
                .setEphemeral(true).queue()
        }
    }
}