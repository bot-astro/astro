package space.astro.bot.commands.vc.settings

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis

@Component
class LimitCommand(
    contextResolverFactory: VcContextResolverFactory,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val LIMIT_OPTION = "limit"
    }

    override val data =
        Commands
            .slash("limit", "Set the user limit for your VC")
            .setContexts(InteractionContextType.GUILD)
            .addOptions(
                OptionData(OptionType.INTEGER, LIMIT_OPTION, "The user limit for your VC (0 for no limit)", true)
                    .setMaxValue(99),
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
        val limit = event.getOption(LIMIT_OPTION)?.asInt
        val commandsSettings = ctx.vcOperationCTX.generatorSettings.commandsSettings
        val minUserLimit = commandsSettings.minUserLimit ?: 0
        val maxUserLimit = commandsSettings.maxUserLimit ?: 99

        if (limit == null || limit < minUserLimit || limit > maxUserLimit) {
            ctx.responder.error(
                "User limit must be between `$minUserLimit` and `$maxUserLimit`." +
                    "\n(*Those limits were set by the moderators of the server*)",
            )
            return
        }

        val vcCtx = ctx.vcOperationCTX
        vcCtx.temporaryVCManager.setUserLimit(limit)
        vcCtx.markTemporaryVCManagerAsUpdated()
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(Containers.default("${Emojis.limit.formatted} User limit set to $limit users!"))
    }
}
