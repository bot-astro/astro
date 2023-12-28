package space.astro.bot.interactions.command.impl.vc.ownership

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "claim",
    description = "Ask to get the ownership of the voice channel so that you can manage it",
    category = CommandCategory.VC
)
class ClaimCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        TODO("Whole cmd")
    }
}