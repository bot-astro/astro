package space.astro.bot.commands.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.Embeds
import space.astro.bot.ui.Emojis

@Component
class BitrateCommand(
    contextResolverFactory: VcContextResolverFactory,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val BITRATE_OPTION = "bitrate"
    }

    override val data =
        Commands
            .slash("bitrate", "Set the bitrate for your VC")
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData("kbps", "Set the bitrate in KBPS")
                    .addOptions(
                        OptionData(OptionType.INTEGER, BITRATE_OPTION, "The bitrate expressed in kbps", true)
                            .setMinValue(8),
                    ),
            )
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.UNKNOWN,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val bitrate = event.getOption(BITRATE_OPTION)?.asInt?.times(1000)
        val commandsSettings = ctx.vcOperationCTX.generatorSettings.commandsSettings
        val maxBitrate = commandsSettings.maxBitrate?.coerceAtMost(ctx.guild.maxBitrate) ?: ctx.guild.maxBitrate
        val minBitrate = (commandsSettings.minBitrate ?: 8000).coerceAtLeast(8000)

        if (bitrate == null || bitrate < minBitrate || bitrate > maxBitrate) {
            ctx.responder.error(
                "The bitrate must be between `${minBitrate / 1000} kbps` and `${maxBitrate / 1000} kbps`!" +
                    "\n(*Those bounds were set by the moderators of the server*)",
            )
            return
        }

        val vcCtx = ctx.vcOperationCTX
        vcCtx.temporaryVCManager.setBitrate(bitrate)
        vcCtx.markTemporaryVCManagerAsUpdated()
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(Embeds.default("${Emojis.bitrate.formatted} Bitrate set to ${bitrate / 1000}kbps"))
    }
}
