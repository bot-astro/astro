package space.astro.bot.interactions.handlers.command.impl.vc.ownership

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao

@Command(
    name = "transfer",
    description = "Transfer the ownership of your VC to someone else",
    category = CommandCategory.VC,
    action = InteractionAction.VC_TRANSFER
)
class TransferCommand(
    private val vcOwnershipManager: VCOwnershipManager,
    private val temporaryVCDao: TemporaryVCDao
) : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
        @CommandOption(
            name = "user",
            description = "The user that will get ownership of the VC",
            type = OptionType.USER
        )
        member: Member?
    ) {
        if (member == null) {
            ctx.replyHandler.replyEmbed(Embeds.error("The user you provided is not in this server!"))
            return
        }

        if (member.user.isBot) {
            ctx.replyHandler.replyEmbed(Embeds.error("Cannot transfer the ownership to a bot"))
            return
        }

        if (member.voiceState!!.channel?.id != ctx.vcOperationCTX.temporaryVC.id) {
            ctx.replyHandler.replyEmbed(Embeds.error("The user you provided is not in your voice channel!"))
            return
        }

        ctx.replyHandler.deferReply()

        vcOwnershipManager.changeOwner(ctx.vcOperationCTX, member)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
        vcOwnershipManager.handleOwnerRoleMigration(ctx.vcOperationCTX, ctx.member, member)

        ctx.replyHandler.replyEmbed(Embeds.default("Ownership transferred to ${member.asMention}"))
    }
}