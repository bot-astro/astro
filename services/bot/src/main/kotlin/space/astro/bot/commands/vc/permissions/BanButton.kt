package space.astro.bot.commands.vc.permissions

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers

/**
 * Replies with a menu handled by [BanMenu]
 */
@Component
class BanButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/ban"
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val menu =
            EntitySelectMenu
                .create(BanMenu.MENU_ID, listOf(EntitySelectMenu.SelectTarget.USER, EntitySelectMenu.SelectTarget.ROLE))
                .setPlaceholder(ctx.t("vc.ban.placeholder"))
                .setRequiredRange(1, 5)
                .build()

        ctx.responder.reply(
            Containers.default(
                ctx.t("vc.ban.menu"),
                ActionRow.of(menu),
            ),
        )
    }
}
