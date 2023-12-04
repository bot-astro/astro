package space.astro.bot.interactions.command.impl.vc.ownership

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "transfer",
    description = "Transfer the ownership of your VC to someone else",
    category = CommandCategory.VC
)
class TransferCommand : AbstractCommand() {
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
                .setEphemeral(true).queue()
            return
        }

        TODO()
    }
}