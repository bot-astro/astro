package space.astro.bot.commands.developer

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Messages

/**
 * Sends in the current channel messages that Astro normally sends on its own and are hard to trigger on demand,
 * to check how they look on Discord. Messages that can be tested by running a command don't belong here.
 * The messages themselves live in [Messages].
 *
 * To add one, add an entry to [DevMessage] and its case in [handle], it shows up in the command choices.
 */
@Component
class DevMessageCommand : SlashHandler<InteractionContext> {
    private enum class DevMessage(
        val label: String,
    ) {
        WELCOME("Welcome (guild join)"),
        VC_LOG_JOINED("VC log: member joined"),
        VC_LOG_LEFT("VC log: member left"),
    }

    override val data =
        Commands
            .slash("dev-message", CommandLocalization.description("dev-message"))
            .setContexts(InteractionContextType.GUILD)
            .addOptions(
                OptionData(OptionType.STRING, "type", CommandLocalization.description("dev-message.type"), true)
                    .addChoices(DevMessage.entries.map { Command.Choice(it.label, it.name) }),
            )
    override val settings = InteractionSettings(ack = Ack.NONE, developerOnly = true)
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) {
        val message = DevMessage.valueOf(event.getOption("type")!!.asString)

        val guild = ctx.guild
        val mention = event.user.asMention
        val data =
            when (message) {
                DevMessage.WELCOME -> Messages.welcome(guild, ctx.locale)
                DevMessage.VC_LOG_JOINED -> Messages.vcLog(guild, ctx.locale, "vc.log.joined", "user" to mention)
                DevMessage.VC_LOG_LEFT -> Messages.vcLog(guild, ctx.locale, "vc.log.left", "user" to mention)
            }

        event.channel.sendMessage(data).await()

        ctx.responder.reply(Containers.default(ctx.t("dev.message.sent", "type" to message.label)))
    }
}
