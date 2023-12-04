package space.astro.bot.interactions.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "template",
    description = "Apply a template to your VC",
    category = CommandCategory.VC
)
class BanCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        TODO()
    }
}