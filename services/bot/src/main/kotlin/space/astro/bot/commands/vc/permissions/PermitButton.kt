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
import space.astro.bot.ui.Embeds

/**
 * Replies with a menu handled by [PermitMenu]
 */
@Component
class PermitButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/permit"
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
                .create(PermitMenu.MENU_ID, listOf(EntitySelectMenu.SelectTarget.USER, EntitySelectMenu.SelectTarget.ROLE))
                .setPlaceholder("Choose the users and roles to permit in your voice channel")
                .setRequiredRange(1, 10)
                .build()

        ctx.responder.reply(
            Embeds.default("Choose the users and roles to permit in your voice channel via the menu below"),
            listOf(ActionRow.of(menu)),
        )
    }
}
