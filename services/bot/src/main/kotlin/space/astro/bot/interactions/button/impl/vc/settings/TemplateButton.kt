package space.astro.bot.interactions.button.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import space.astro.bot.components.managers.vc.VCTemplateManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.button.AbstractButton
import space.astro.bot.interactions.button.Button
import space.astro.bot.interactions.button.ButtonRunnable
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao

@Button(
    id = InteractionIds.Button.VC_TEMPLATE,
    action = InteractionAction.VC_TEMPLATE
)
class TemplateButton(
    private val guildDao: GuildDao,
    private val vcTemplateManager: VCTemplateManager,
    private val temporaryVCDao: TemporaryVCDao,
    private val interactionComponentBuilder: InteractionComponentBuilder
): AbstractButton() {
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
            event.replyEmbeds(Embeds.error("This generator doesn't have any available template"))
                .setEphemeral(true)
                .queue()

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

        event.replyEmbeds(Embeds.default("Choose a template with the menu below"))
            .setActionRow(templateSelectMenu)
            .queue()
    }
}