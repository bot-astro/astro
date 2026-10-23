package space.astro.bot.commands.info

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.properties.PodProperties
import space.astro.bot.ui.Buttons
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.bot.ui.Links
import space.astro.shared.core.properties.ShardManagerConfig

@Component
class HelpCommand(
    private val podProperties: PodProperties,
    private val shardManagerConfig: ShardManagerConfig,
) : SlashHandler<InteractionContext> {
    override val data =
        Commands
            .slash("help", "Get help with Astro")
            .setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY)
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) {
        val shardInfo = event.jda.shardInfo
        val dashboard = Links.guildDashboard(ctx.guildId)

        // TODO: add the Ultimate field and button once premium is implemented
        ctx.responder.reply(
            Containers.container(
                Containers.purple,
                Containers.title("${Emojis.help.formatted} Help"),
                Containers.text("Astro is a bot that focuses on the voice aspect of Discord, and takes it to the next level with some quality features."),
                Containers.divider(),
                Containers.field(
                    "Features",
                    "Astro provides the following features:" +
                        "\n${Emojis.generator.formatted} *Temporary voice channels*" +
                        "\n${Emojis.voiceRole.formatted} *Voice roles*" +
                        "\n${Emojis.vcInterface.formatted} *Interfaces*" +
                        "\n${Emojis.template.formatted} *Templates*" +
                        "\nTo find out all the cool things you can do with Astro, take a look at the [`Guides`](${Links.GUIDES})!",
                ),
                Containers.field(
                    "Setup & Configuration",
                    "Use the ${Emojis.dashboard.formatted} [`Dashboard`]($dashboard) to setup Astro and configure its settings!",
                ),
                Containers.field(
                    "Errors & Issues",
                    "The ${Emojis.dashboard.formatted} [`Dashboard`]($dashboard) also shows all the errors Astro encounters when working in your server. If something is not working as expected, check the dashboard!",
                ),
                Containers.field(
                    "Support",
                    "You can join the ${Emojis.helper.formatted} [`Support server`](${Links.SUPPORT_SERVER}) to ask questions if something is not quite clear to you!",
                ),
                Containers.field(
                    "Open Source",
                    "Astro is now open source! You can find the code on [`GitHub`](${Links.GITHUB}) and a story of the project on my [`blog article`](${Links.BLOG_ARTICLE})",
                ),
                Containers.divider(),
                ActionRow.of(
                    Button.link(dashboard, "Dashboard").withEmoji(Emojis.dashboard),
                    Buttons.guides,
                    Buttons.support,
                    Buttons.github,
                ),
                Containers.footer(
                    "Thank you for using Astro • shard ${shardInfo.shardId}/${shardInfo.shardTotal} • " +
                        "pod ${podProperties.getParsedOrdinal()}/${shardManagerConfig.totalPods}",
                ),
            ),
        )
    }
}
