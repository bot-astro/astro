package space.astro.bot.commands.vc.ownership

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis

/**
 * Replies with a menu handled by [TransferMenu]
 */
@Component
class TransferButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/transfer"
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
        val members =
            ctx.vcOperationCTX.temporaryVC.members
                .filter { !it.user.isBot && it.id != ctx.memberId }

        if (members.isEmpty()) {
            ctx.responder.error("vc.transfer.no_candidates")
            return
        }

        val menu =
            StringSelectMenu
                .create(TransferMenu.MENU_ID)
                .setPlaceholder(ctx.t("vc.transfer.placeholder"))
                .addOptions(
                    members.take(25).map {
                        SelectOption
                            .of(it.effectiveName, it.id)
                            .withDescription(it.user.name)
                            .withEmoji(Emojis.user)
                    },
                ).build()

        ctx.responder.reply(
            Containers.default(
                ctx.t("vc.transfer.menu"),
                ActionRow.of(menu),
            ),
        )
    }
}
