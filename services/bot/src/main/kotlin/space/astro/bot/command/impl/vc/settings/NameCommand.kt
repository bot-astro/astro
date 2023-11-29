package space.astro.bot.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.*
import space.astro.bot.components.managers.vc.VCNameManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "name",
    description = "Set the name for your VC",
    category = CommandCategory.VC
)
class NameCommand(
    val vcNameManager: VCNameManager
) : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcCommandContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.USER_RENAME
        )
        ctx: VcCommandContext,
        @CommandOption(
            description = "The name for your VC",
            minLength = 2,
            maxLength = 100
        )
        name: String
    ) {
        vcNameManager.performVCRename(ctx.vcOperationCTX, name)

        // TODO: React to possible issues

        ctx.vcOperationCTX.queueUpdatedManagers()

        event.replyEmbeds(Embeds.default(
            "${Emojis.name.formatted} Name of your VC set to `$name`!"
        )).setEphemeral(true).queue()
    }
}