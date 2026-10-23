package space.astro.bot.commands.vc.template

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCTemplateManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.interactions.handlers.AutocompleteHandler
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class TemplateCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcTemplateManager: VCTemplateManager,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val guildSettingsRepository: GuildSettingsRepository,
) : SlashHandler<VcInteractionContext>,
    AutocompleteHandler {
    companion object {
        private const val TEMPLATE_OPTION = "template"
    }

    override val command = "template"

    override val data =
        Commands
            .slash("template", "Apply a template to your VC")
            .setContexts(InteractionContextType.GUILD)
            .addOption(
                OptionType.STRING,
                TEMPLATE_OPTION,
                "The name of the template to use, leave blank to see all available templates",
                false,
                true,
            )
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(event: CommandAutoCompleteInteractionEvent) {
        val guildId = event.guild?.id ?: return
        val templates = withContext(Dispatchers.IO) { guildSettingsRepository.findByGuildID(guildId)?.templates } ?: return

        event
            .replyChoices(
                templates
                    .filter { it.name.startsWith(event.focusedOption.value, true) }
                    .take(25)
                    .map { Command.Choice(it.name.take(100), it.id) },
            ).queue()
    }

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
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

        val templateInput = event.getOption(TEMPLATE_OPTION)?.asString

        if (templateInput == null) {
            ctx.responder.reply(
                Containers.default(
                    "Choose a template with the menu below",
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
            return
        }

        val template = availableTemplates.firstOrNull { it.id == templateInput || it.name.equals(templateInput, true) }

        if (template == null) {
            ctx.responder.error("Couldn't find a template matching $templateInput")
            return
        }

        vcTemplateManager.applyTemplate(vcCtx, template)
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, vcCtx.temporaryVCEntity) }
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(Containers.default("Template applied!"))
    }
}
