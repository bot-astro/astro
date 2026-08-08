package space.astro.bot.interactions.handlers.menu.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import space.astro.bot.components.managers.vc.VCTemplateManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.menu.AbstractMenu
import space.astro.bot.interactions.handlers.menu.Menu
import space.astro.bot.interactions.handlers.menu.MenuRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao

@Menu(
    id = InteractionIds.Menu.VC_TEMPLATE,
    action = InteractionAction.VC_TEMPLATE
)
class TemplateMenu(
    private val vcTemplateManager: VCTemplateManager,
    private val temporaryVCDao: TemporaryVCDao
) : AbstractMenu() {

    @MenuRunnable
    suspend fun run(
        event: StringSelectInteractionEvent,
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

        val template = availableTemplates.firstOrNull { it.id == event.values.firstOrNull() }
        if (template == null) {
            ctx.replyHandler.replyEmbed(Embeds.error("The template you selected doesn't exist anymore"))
            return
        }

        ctx.replyHandler.deferReply()

        vcTemplateManager.applyTemplate(ctx.vcOperationCTX, template)
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
        ctx.vcOperationCTX.queueUpdatedManagers()

        ctx.replyHandler.replyEmbed(Embeds.default("Template applied!"))
    }
}