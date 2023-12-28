package space.astro.bot.interactions.command.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import space.astro.bot.components.managers.vc.VCTemplateManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao

@Command(
    name = "template",
    description = "Apply a template to your VC",
    category = CommandCategory.VC
)
class TemplateCommand(
    private val guildDao: GuildDao,
    private val vcTemplateManager: VCTemplateManager,
    private val temporaryVCDao: TemporaryVCDao
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
            event.replyEmbeds(Embeds.error("This generator doesn't have any available template"))
                .setEphemeral(true)
                .queue()

            return
        }

        if (template != null) {
            val templateChosen = availableTemplates.firstOrNull { it.id == template }

            if (templateChosen != null) {
                vcTemplateManager.applyTemplate(ctx.vcOperationCTX, templateChosen)
                temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
                ctx.vcOperationCTX.queueUpdatedManagers()

                event.replyEmbeds(Embeds.default("Template applied!"))
                    .setEphemeral(true)
                    .queue()
            } else {
                event.replyEmbeds(Embeds.error("Couldn't find a template matching $template"))
                    .setEphemeral(true)
                    .queue()
            }
        } else {
            TODO("Reply with template button")
        }
    }
}