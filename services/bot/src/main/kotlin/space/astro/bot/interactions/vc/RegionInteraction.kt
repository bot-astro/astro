package space.astro.bot.interactions.vc

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Embeds

/**
 * `/region` and the region button: both reply with a menu handled by [RegionMenu]
 */
@Component
class RegionInteraction(
    contextResolverFactory: VcContextResolverFactory,
) : SlashHandler<VcInteractionContext>,
    ButtonHandler<VcInteractionContext> {
    companion object {
        const val BUTTON_ID = "button/vc/region"
    }

    override val id = BUTTON_ID
    override val data = Commands.slash("region", "Set the region for your VC").setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) = replyWithMenu(ctx)

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: VcInteractionContext,
    ) = replyWithMenu(ctx)

    private suspend fun replyWithMenu(ctx: VcInteractionContext) {
        val regions = ctx.guild.retrieveRegions(false).await()

        val menu =
            StringSelectMenu
                .create(RegionMenu.MENU_ID)
                .setPlaceholder("Set the region for your VC")
                .addOptions(
                    regions.map { region ->
                        SelectOption
                            .of(region.getName(), region.key)
                            .withEmoji(region.emoji?.let { Emoji.fromUnicode(it) })
                    },
                ).build()

        ctx.responder.reply(
            Embeds.default("Set the region of your VC with the menu below"),
            listOf(ActionRow.of(menu)),
        )
    }
}
