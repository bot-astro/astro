package space.astro.bot.commands.vc.template

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCTemplateManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.interactions.handlers.StringSelectHandler
import space.astro.bot.ui.Containers
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class TemplateMenu(
    contextResolverFactory: VcContextResolverFactory,
    private val vcTemplateManager: VCTemplateManager,
    private val temporaryVCRepository: TemporaryVCRepository,
) : StringSelectHandler<VcInteractionContext> {
    companion object {
        const val MENU_ID = "menu/vc/template"
    }

    override val id = MENU_ID
    override val settings = InteractionSettings(ack = Ack.DEFER_EDIT)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: StringSelectInteractionEvent,
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

        val template = availableTemplates.firstOrNull { it.id == event.values.firstOrNull() }

        if (template == null) {
            ctx.responder.error("The template you selected doesn't exist anymore")
            return
        }

        vcTemplateManager.applyTemplate(vcCtx, template)
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, vcCtx.temporaryVCEntity) }
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(Containers.default("Template applied!"))
    }
}
