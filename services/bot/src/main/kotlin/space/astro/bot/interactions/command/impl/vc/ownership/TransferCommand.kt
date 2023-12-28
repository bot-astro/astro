package space.astro.bot.interactions.command.impl.vc.ownership

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.TemporaryVCDao

@Command(
    name = "transfer",
    description = "Transfer the ownership of your VC to someone else",
    category = CommandCategory.VC
)
class TransferCommand(
    private val vcOwnershipManager: VCOwnershipManager,
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val configurationErrorService: ConfigurationErrorService,
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
            event.replyEmbeds(Embeds.error("The user you provided is not in this server!"))
                .setEphemeral(true)
                .queue()

            return
        }

        if (member.user.isBot) {
            event.replyEmbeds(Embeds.error("Cannot transfer the ownership to a bot"))
                .setEphemeral(true)
                .queue()

            return
        }

        if (member.voiceState!!.channel?.id != ctx.vcOperationCTX.temporaryVC.id) {
            event.replyEmbeds(Embeds.error("The user you provided is not in your voice channel!"))
                .setEphemeral(true)
                .queue()

            return
        }

        event.deferReply(true).await()

        vcOwnershipManager.changeOwner(ctx.vcOperationCTX, member)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.vcOperationCTX.generatorData.ownerRole?.let { ownerRoleId ->
            ctx.guild.getRoleById(ownerRoleId)?.let { ownerRole ->
                if (premiumRequirementDetector.canAssignTemporaryVCOwnerRole(ctx.vcOperationCTX.guildData)) {
                    if (ctx.member.roles.any { it.id == ownerRole.id }) {
                        ctx.guild.removeRoleFromMember(ctx.user, ownerRole).queue()
                        ctx.guild.addRoleToMember(member.user, ownerRole).queue()
                    }
                } else {
                    throw ConfigurationException(configurationErrorService.premiumRequiredForOwnerRole())
                }
            }
        }

        event.hook.editOriginalEmbeds(Embeds.default("Ownership transferred to ${member.asMention}"))
            .queue()
    }
}