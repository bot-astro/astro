package space.astro.bot.interactions.handlers.command.impl.info

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import space.astro.bot.components.discord.ShardManagerConfig
import space.astro.bot.config.PodConfig
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.SubCommand
import space.astro.shared.core.configs.KubeConfig


@Command(
    name = "help",
    description = "learn how to use Astro"
)
class HelpCommand(
    private val podConfig: PodConfig,
    private val shardManagerConfig: ShardManagerConfig
) : AbstractCommand() {
    @SubCommand(
        name = "general",
        description = "Starting point to learn how to use Astro"
    )
    suspend fun general(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.reply(
            embed = Embeds.helpGeneral(
                shardId = event.jda.shardInfo.shardId,
                shardCount = event.jda.shardInfo.shardTotal,
                podId = podConfig.getParsedOrdinal(),
                podCount = shardManagerConfig.totalPods
            ),
            components = listOf(
                ActionRow.of(Buttons.Help.premium, Buttons.Help.generators, Buttons.Help.connections),
                ActionRow.of(Buttons.Help.variables, Buttons.Help.interfaces, Buttons.Help.templates)
            )
        )
    }

    @SubCommand(
        name = "premium",
        description = "Discover the premium version of Astro and its features"
    )
    suspend fun premium(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.helpPremium,
            component = Buttons.premium
        )
    }

    @SubCommand(
        name = "variables",
        description = "Discover variables that can be used in temporary voice channel names"
    )
    suspend fun variables(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.reply(
            embed = Embeds.helpVariables,
            components = listOf(ActionRow.of(Buttons.Bundles.helpAndLinks))
        )
    }

    @SubCommand(
        name = "generators",
        description = "Learn how to configure generators for temporary voice channels"
    )
    suspend fun generators(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.reply(
            embed = Embeds.helpGenerators,
            components = listOf(ActionRow.of(Buttons.Bundles.helpAndLinks))
        )
    }

    @SubCommand(
        name = "interfaces",
        description = "Learn how to configure interfaces for temporary voice channels"
    )
    suspend fun interfaces(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.reply(
            embed = Embeds.helpInterfaces,
            components = listOf(ActionRow.of(Buttons.Bundles.helpAndLinks))
        )
    }

    @SubCommand(
        name = "templates",
        description = "Learn how to configure templates for temporary voice channels"
    )
    suspend fun templates(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.reply(
            embed = Embeds.helpTemplates,
            components = listOf(ActionRow.of(Buttons.Bundles.helpAndLinks))
        )
    }

    @SubCommand(
        name = "connections",
        description = "Learn how to connect voice channels to roles"
    )
    suspend fun connections(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.reply(
            embed = Embeds.helpConnections,
            components = listOf(ActionRow.of(Buttons.Bundles.helpAndLinks))
        )
    }
}