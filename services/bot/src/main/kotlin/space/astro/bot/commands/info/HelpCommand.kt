package space.astro.bot.commands.info

import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.properties.PodProperties
import space.astro.bot.ui.Buttons
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.bot.ui.Links
import space.astro.shared.core.properties.bot.BotShardProperties

@Component
class HelpCommand(
    private val podProperties: PodProperties,
    private val botShardProperties: BotShardProperties,
) : SlashHandler<InteractionContext> {
    override val data =
        Commands
            .slash("help", CommandLocalization.description("help"))
            .setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.NONE)
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) {
        val shardInfo = event.jda.shardInfo
        val dashboard = Links.guildDashboard(ctx.guildId)

        ctx.responder.reply(
            Containers.container(
                Containers.purple,
                Containers.title(ctx.t("help.title", "emoji" to Emojis.help.formatted)),
                Containers.text(ctx.t("help.description")),
                Containers.divider(),
                Containers.field(
                    ctx.t("help.features.title"),
                    ctx.t(
                        "help.features.content",
                        "generatorEmoji" to Emojis.generator.formatted,
                        "voiceRoleEmoji" to Emojis.voiceRole.formatted,
                        "interfaceEmoji" to Emojis.vcInterface.formatted,
                        "templateEmoji" to Emojis.template.formatted,
                        "guidesLink" to Links.GUIDES,
                    ),
                ),
                Containers.field(
                    ctx.t("help.setup.title"),
                    ctx.t("help.setup.content", "emoji" to Emojis.dashboard.formatted, "link" to dashboard),
                ),
                Containers.field(
                    ctx.t("help.errors.title"),
                    ctx.t("help.errors.content", "emoji" to Emojis.dashboard.formatted, "link" to dashboard),
                ),
                Containers.field(
                    ctx.t("help.ultimate.title"),
                    ctx.t("help.ultimate.content", "emoji" to Emojis.premium.formatted, "link" to Links.ULTIMATE),
                ),
                Containers.field(
                    ctx.t("help.support.title"),
                    ctx.t("help.support.content", "emoji" to Emojis.helper.formatted, "link" to Links.SUPPORT_SERVER),
                ),
                Containers.field(
                    ctx.t("help.open_source.title"),
                    ctx.t("help.open_source.content", "githubLink" to Links.GITHUB, "blogLink" to Links.BLOG_ARTICLE),
                ),
                Containers.divider(),
                Containers.footer(
                    ctx.t(
                        "help.footer",
                        "shardId" to shardInfo.shardId.toString(),
                        "shardTotal" to shardInfo.shardTotal.toString(),
                        "pod" to podProperties.getParsedOrdinal().toString(),
                        "totalPods" to botShardProperties.totalPods.toString(),
                    ),
                ),
            ),
            // buttons sit below the container, like in the welcome message
            *listOfNotNull<MessageTopLevelComponent>(
                // quick setup buttons are only useful while the guild has no generator
                if (ctx.guildSettings?.generators.isNullOrEmpty()) {
                    ActionRow.of(
                        Buttons.setup(ctx.locale),
                        Buttons.createGenerator(ctx.locale),
                        Buttons.createInterface(ctx.locale),
                    )
                } else {
                    null
                },
                ActionRow.of(
                    Buttons.guildDashboard(ctx.locale, ctx.guildId),
                    Buttons.ultimate(ctx.locale),
                    Buttons.guides(ctx.locale),
                    Buttons.support(ctx.locale),
                    Buttons.github(ctx.locale),
                ),
            ).toTypedArray(),
            ephemeral = false,
        )
    }
}
