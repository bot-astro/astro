package space.astro.bot.interactions.handlers.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "bitrate",
    description = "Set the bitrate for your VC",
    category = CommandCategory.VC,
    action = InteractionAction.VC_BITRATE
)
class BitrateCommand : AbstractCommand() {
    @SubCommand(
        name = "kbps",
        description = "Set the bitrate in KBPS"
    )
    suspend fun kbps(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext,
        @CommandOption(
            description = "The bitrate expressed in kbps",
            minValue = 8,
            type =  OptionType.INTEGER
        )
        bitrate: Int
    ) {
        val calculatedBitrate = bitrate * 1000
        val maxBitrate = ctx.vcOperationCTX.generatorData.commandsSettings.maxBitrate?.coerceAtMost(ctx.guild.maxBitrate) ?: ctx.guild.maxBitrate
        val minBitrate = ctx.vcOperationCTX.generatorData.commandsSettings.minBitrate.coerceAtLeast(8000)

        if (calculatedBitrate < minBitrate || calculatedBitrate > maxBitrate) {
            ctx.replyHandler.replyEmbed(Embeds.error(
                "The bitrate must be between `${minBitrate / 1000} kbps` and `${maxBitrate / 1000} kbps`!" +
                        "\n(*Those bounds were set by the moderators of the server*)"
            ))
            return
        }

        ctx.replyHandler.deferReply()

        ctx.vcOperationCTX.temporaryVCManager.setBitrate(calculatedBitrate).queue()

        ctx.replyHandler.replyEmbed(Embeds.default(
            "${Emojis.bitrate.formatted} Bitrate set to ${calculatedBitrate / 1000}kbps"
        ))
    }
}