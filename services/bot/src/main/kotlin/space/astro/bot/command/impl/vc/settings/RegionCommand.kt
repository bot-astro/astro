package space.astro.bot.command.impl.vc.settings

import net.dv8tion.jda.api.Region
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "region",
    description = "Set the region for your VC",
    category = CommandCategory.VC
)
class RegionCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcCommandContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcCommandContext,
    ) {

    }
}