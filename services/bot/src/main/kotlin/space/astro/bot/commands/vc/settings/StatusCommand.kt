package space.astro.bot.commands.vc.settings

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.channel.attribute.IVoiceStatusChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
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
class StatusCommand(
    contextResolverFactory: VcContextResolverFactory,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val STATUS_OPTION = "status"
    }

    override val data =
        Commands
            .slash("status", CommandLocalization.description("status"))
            .setContexts(InteractionContextType.GUILD)
            .addOptions(
                OptionData(OptionType.STRING, STATUS_OPTION, CommandLocalization.description("status.status"), false)
                    .setMaxLength(IVoiceStatusChannel.MAX_STATUS_LENGTH),
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
        // omitting the option unsets the status
        val status = event.getOption(STATUS_OPTION)?.asString?.trim().orEmpty()

        ctx.vcOperationCTX.temporaryVC
            .modifyStatus(status)
            .await()

        val reply =
            if (status.isEmpty()) {
                ctx.t("vc.status.unset", "emoji" to Emojis.logs.formatted)
            } else {
                ctx.t("vc.status.set", "emoji" to Emojis.logs.formatted, "status" to status)
            }

        ctx.responder.reply(Containers.default(reply))
    }
}
