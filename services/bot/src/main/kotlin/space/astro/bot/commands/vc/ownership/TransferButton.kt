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
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.ui.Embeds
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
        val members = ctx.vcOperationCTX.temporaryVC.members.filter { !it.user.isBot && it.id != ctx.memberId }

        if (members.isEmpty()) {
            ctx.responder.error(
                "Cannot find a user in your voice channel that is not a bot.\nRun this command again when someone joins your voice channel",
            )
            return
        }

        val menu =
            StringSelectMenu
                .create(TransferMenu.MENU_ID)
                .setPlaceholder("Choose the new owner of your voice channel")
                .addOptions(
                    members.take(25).map {
                        SelectOption
                            .of(it.effectiveName, it.id)
                            .withDescription(it.user.name)
                            .withEmoji(Emojis.user)
                    },
                ).build()

        ctx.responder.reply(
            Embeds.default("Choose the new owner of your voice channel with the menu below"),
            listOf(ActionRow.of(menu)),
        )
    }
}
