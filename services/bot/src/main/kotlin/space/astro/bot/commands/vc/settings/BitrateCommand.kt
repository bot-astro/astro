package space.astro.bot.commands.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
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
            .slash("bitrate", CommandLocalization.description("bitrate"))
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData("kbps", CommandLocalization.description("bitrate.kbps"))
                    .addOptions(
                        OptionData(OptionType.INTEGER, BITRATE_OPTION, CommandLocalization.description("bitrate.kbps.bitrate"), true)
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
                "vc.bitrate.out_of_range",
                "min" to (minBitrate / 1000).toString(),
                "max" to (maxBitrate / 1000).toString(),
            )
            return
        }

        val vcCtx = ctx.vcOperationCTX
        vcCtx.temporaryVCManager.setBitrate(bitrate)
        vcCtx.markTemporaryVCManagerAsUpdated()
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(
            Containers.default(ctx.t("vc.bitrate.set", "emoji" to Emojis.bitrate.formatted, "bitrate" to (bitrate / 1000).toString())),
        )
    }
}
