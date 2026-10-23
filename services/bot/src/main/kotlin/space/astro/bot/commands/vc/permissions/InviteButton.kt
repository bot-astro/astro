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
 * Replies with a menu handled by [InviteMenu]
 */
@Component
class InviteButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/invite"
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.UNKNOWN,
        )

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val menu =
            EntitySelectMenu
                .create(InviteMenu.MENU_ID, listOf(EntitySelectMenu.SelectTarget.USER))
                .setPlaceholder("Choose the users to invite in your voice channel")
                .setRequiredRange(1, 3)
                .build()

        ctx.responder.reply(
            Containers.default(
                "Choose the users to invite in your voice channel with the menu below",
                ActionRow.of(menu),
            ),
        )
    }
}
