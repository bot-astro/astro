package space.astro.bot.interactions.handlers.button.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_TEMPLATE,
    action = InteractionAction.VC_TEMPLATE
)
class TemplateButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
): space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        val availableTemplates = ctx.vcOperationCTX.guildData.templates.filter { it.enabledGeneratorIds == null || ctx.vcOperationCTX.generatorData.id in it.enabledGeneratorIds!! }

        if (availableTemplates.isEmpty()) {
            ctx.replyHandler.replyEmbed(Embeds.error("This generator doesn't have any available template"))
            return
        }

        val templateSelectMenu = interactionComponentBuilder.selectMenu(
            id = InteractionIds.Menu.VC_TEMPLATE,
            placeholder = "Use a template for your VC",
            options = availableTemplates.map { template ->
                SelectOption.of(template.name, template.id)
                    .withEmoji(Emojis.template)
            },
        )

        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.default("Choose a template with the menu below"),
            component = templateSelectMenu
        )
    }
}