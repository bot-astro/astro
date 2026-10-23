package space.astro.bot.commands.vc.template

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
 * Replies with a menu handled by [TemplateMenu]
 */
@Component
class TemplateButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/template"
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
        val vcCtx = ctx.vcOperationCTX
        val availableTemplates =
            vcCtx.guildSettings.templates.filter {
                it.enabledGeneratorIds == null || vcCtx.generatorSettings.id in it.enabledGeneratorIds!!
            }

        if (availableTemplates.isEmpty()) {
            ctx.responder.error("This generator doesn't have any available template")
            return
        }

        ctx.responder.reply(
            Embeds.default("Choose a template with the menu below"),
            listOf(
                ActionRow.of(
                    StringSelectMenu
                        .create(TemplateMenu.MENU_ID)
                        .setPlaceholder("Use a template for your VC")
                        .addOptions(
                            availableTemplates.take(25).map { template ->
                                SelectOption.of(template.name, template.id).withEmoji(Emojis.template)
                            },
                        ).build(),
                ),
            ),
        )
    }
}
