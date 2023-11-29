package space.astro.bot.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.*

@Command(
    name = "ban",
    description = "Bans someone from joining your voice channel",
    category = CommandCategory.VC
)
class BanCommand : AbstractCommand() {
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