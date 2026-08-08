package space.astro.bot.interactions.handlers.command.impl.vc.permissions

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "permit",
    description = "Permit a user or role to join your VC",
    category = CommandCategory.VC,
    action = InteractionAction.VC_PERMIT
)
class PermitCommand(
    private val vcPermissionManager: VCPermissionManager
) : AbstractCommand() {
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
        ctx.replyHandler.deferReply()

        vcPermissionManager.permit(
            vcOperationCTX = ctx.vcOperationCTX,
            memberIds = listOf(member.idLong),
            roles = emptyList()
        )

        ctx.replyHandler.replyEmbed(Embeds.default("${member.asMention} can now join your VC!"))
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
            type = OptionType.ROLE,
            name = "role",
            description = "The role to ban from your channel"
        ) role: Role,
    ) {
        ctx.replyHandler.deferReply()

        vcPermissionManager.permit(
            vcOperationCTX = ctx.vcOperationCTX,
            memberIds = emptyList(),
            roles = listOf(role)
        )

        ctx.replyHandler.replyEmbed(Embeds.default("${role.asMention} role can now join your temporary VC!"))
    }
}