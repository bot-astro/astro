package space.astro.bot.interactions.handlers.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import space.astro.bot.components.managers.vc.VCTemplateManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao

@Command(
    name = "template",
    description = "Apply a template to your VC",
    category = CommandCategory.VC,
    action = InteractionAction.VC_TEMPLATE
)
class TemplateCommand(
    private val guildDao: GuildDao,
    private val vcTemplateManager: VCTemplateManager,
    private val temporaryVCDao: TemporaryVCDao,
    private val interactionComponentBuilder: InteractionComponentBuilder
): AbstractCommand() {
    override fun handleAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        if (!event.isFromGuild) {
            throw IllegalStateException("Received an autocomplete event that required to be in a guild from a command that isn't in a guild, command: template")
        }

        val templates = guildDao.get(event.guild!!.id)?.templates ?: return

        val choices = templates
            .filter { template -> template.name.startsWith(event.focusedOption.value, true) }
            .map { template -> net.dv8tion.jda.api.interactions.commands.Command.Choice(template.name, template.id) }
            .toList()

        event.replyChoices(choices).queue()
    }

    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
        @CommandOption(
            description = "The template to use, leave blank to see all available templates",
            type = OptionType.STRING,
            autocomplete = true
        )
        template: String?
    ) {
        val availableTemplates = ctx.vcOperationCTX.guildData.templates.filter { it.enabledGeneratorIds == null || ctx.vcOperationCTX.generatorData.id in it.enabledGeneratorIds!! }

        if (availableTemplates.isEmpty()) {
            ctx.replyHandler.replyEmbed(Embeds.error("This generator doesn't have any available template"))
            return
        }

        if (template != null) {
            val templateChosen = availableTemplates.firstOrNull { it.id == template }

            if (templateChosen != null) {
                ctx.replyHandler.deferReply()

                vcTemplateManager.applyTemplate(ctx.vcOperationCTX, templateChosen)
                temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
                ctx.vcOperationCTX.queueUpdatedManagers()

                ctx.replyHandler.replyEmbed(Embeds.default("Template applied!"))
            } else {
                ctx.replyHandler.replyEmbed(Embeds.error("Couldn't find a template matching $template"))
            }
        } else {
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
}