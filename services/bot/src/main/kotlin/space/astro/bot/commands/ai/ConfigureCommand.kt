package space.astro.bot.commands.ai

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.springframework.stereotype.Component
import space.astro.bot.components.ai.AiAssistantManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.AiMessages
import space.astro.bot.ui.Containers
import kotlin.time.Duration.Companion.seconds

/**
 * Opens a private thread where the user configures Astro by chatting with the assistant
 */
@Component
class ConfigureCommand(
    private val aiAssistantManager: AiAssistantManager,
) : SlashHandler<InteractionContext> {
    private companion object {
        const val PROMPT_OPTION = "prompt"
        const val PROMPT_MAX_LENGTH = 1000
        const val THREAD_NAME_USER_MAX_LENGTH = 60
    }

    override val data =
        Commands
            .slash("configure", CommandLocalization.description("configure"))
            .setContexts(InteractionContextType.GUILD)
            .addOptions(
                OptionData(OptionType.STRING, PROMPT_OPTION, CommandLocalization.description("configure.prompt"), false)
                    .setMaxLength(PROMPT_MAX_LENGTH),
            )
    override val settings =
        InteractionSettings(
            ack = Ack.DEFER_REPLY_EPHEMERAL,
            cooldown = 10.seconds,
            memberPermissions = setOf(Permission.MANAGE_SERVER),
            botPermissions =
                setOf(
                    Permission.VIEW_CHANNEL,
                    Permission.CREATE_PRIVATE_THREADS,
                    Permission.MESSAGE_SEND_IN_THREADS,
                ),
        )
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) {
        val channel = event.channel as? TextChannel

        if (channel == null) {
            ctx.responder.error("ai.configure.invalid_channel")
            return
        }

        val prompt = event.getOption(PROMPT_OPTION)?.asString?.takeIf { it.isNotBlank() }

        val thread =
            channel
                .createThreadChannel(
                    ctx.t("ai.thread.name", "user" to ctx.member.effectiveName.take(THREAD_NAME_USER_MAX_LENGTH)),
                    true,
                ).setInvitable(false)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS)
                .await()
        thread.addThreadMember(ctx.member).await()
        aiAssistantManager.markThread(thread.id)

        ctx.responder.reply(Containers.default(ctx.t("ai.configure.opened", "thread" to thread.asMention)))

        if (prompt == null) {
            thread.sendMessage(AiMessages.welcome(ctx.locale, ctx.guild.selfMember.asMention)).await()
            return
        }

        thread.sendTyping().queue()
        thread
            .sendMessage(
                aiAssistantManager.respond(ctx.guildId, ctx.memberId, thread.id, prompt, ctx.guildSettings, quote = prompt),
            ).await()
    }
}
