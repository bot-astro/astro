package space.astro.bot.interactions.command.impl.vc.permissions

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.interactions.command.*
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "permit",
    description = "Permit a user or role to join your VC",
    category = CommandCategory.VC
)
class PermitCommand : AbstractCommand() {
    @SubCommand(
        name = "user",
        description = "Permit a user to join your VC"
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
            description = "The user to permit in your channel"
        ) member: Member,
    ) {
        ctx.vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
            member,
            Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
            0
        ).queue()

        event.replyEmbeds(Embeds.default("${member.asMention} can now join your VC!"))
            .setEphemeral(true).queue()
    }

    @SubCommand(
        name = "role",
        description = "The role to permit in your VC"
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

        ctx.vcOperationCTX.temporaryVC.manager.modifyPermissionOverride(
            role,
            Permission.getRaw(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
            0
        ).queue()

        event.replyEmbeds(Embeds.default("${role.asMention} role can now join your temporary VC!"))
            .setEphemeral(true).queue()
    }
}