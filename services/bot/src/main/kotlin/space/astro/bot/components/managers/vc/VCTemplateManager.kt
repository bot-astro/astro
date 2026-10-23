package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.Region
import org.springframework.stereotype.Component
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.interactions.InteractionException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.models.database.guildSettings.TemplateData

@Component
class VCTemplateManager(
    private val vcNameManager: VCNameManager,
) {
    /**
     * Applies a [template], the changes are applied once [VCOperationCTX.queueUpdatedManagers] is called
     *
     * @throws ConfigurationException
     * @throws InteractionException
     */
    suspend fun applyTemplate(
        ctx: VCOperationCTX,
        template: TemplateData,
    ) {
        template.vcName?.let { vcNameManager.performVCRename(ctx, it) }

        template.vcLimit?.let {
            ctx.temporaryVCManager.setUserLimit(it)
            ctx.markTemporaryVCManagerAsUpdated()
        }

        template.vcBitrate?.let {
            ctx.temporaryVCManager.setBitrate(it)
            ctx.markTemporaryVCManagerAsUpdated()
        }

        template.vcRegion?.let {
            ctx.temporaryVCManager.setRegion(Region.fromKey(it))
            ctx.markTemporaryVCManagerAsUpdated()
        }
    }
}
