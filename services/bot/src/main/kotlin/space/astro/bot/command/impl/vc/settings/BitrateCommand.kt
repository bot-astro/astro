package space.astro.bot.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.*
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "bitrate",
    description = "Set the bitrate for your VC",
    category = CommandCategory.VC
)
class BitrateCommand : AbstractCommand() {
    @SubCommand(
        description = "Set the bitrate in KBPS"
    )
    suspend fun kbps(
        event: SlashCommandInteractionEvent,
        @VcCommandContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcCommandContext,
        @CommandOption(
            description = "The bitrate expressed in kbps",
            minValue = 8
        )
        bitrate: Int
    ) {
        val calculatedBitrate = bitrate * 1000;
        val maxBitrate = ctx.vcOperationCTX.generatorData.commandsSettings.maxBitrate?.coerceAtMost(ctx.guild.maxBitrate) ?: ctx.guild.maxBitrate
        val minBitrate = ctx.vcOperationCTX.generatorData.commandsSettings.minBitrate.coerceAtLeast(8000)

        if (calculatedBitrate < minBitrate || calculatedBitrate > maxBitrate) {
            event.replyEmbeds(Embeds.error(
                "The bitrate must be between `${minBitrate / 1000} kbps` and `${maxBitrate / 1000} kbps`!" +
                        "\n(*Those bounds were set by the moderators of the server*)"
            )).setEphemeral(true).queue()
            return
        }

        ctx.vcOperationCTX.temporaryVCManager.setBitrate(calculatedBitrate).queue()

        event.replyEmbeds(Embeds.default(
            "${Emojis.bitrate.formatted} Bitrate set to ${calculatedBitrate / 1000}kbps"
        )).setEphemeral(true).queue()
    }
}