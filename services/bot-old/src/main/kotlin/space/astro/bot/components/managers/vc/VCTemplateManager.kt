package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.Region
import org.springframework.stereotype.Component
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.exceptions.VcOperationException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.models.database.TemplateData

@Component
class VCTemplateManager(
    private val vcNameManager: VCNameManager
) {
    /**
     * Applies a [template]
     *
     * @throws ConfigurationException
     * @throws VcOperationException
     */
    fun applyTemplate(
        vcOperationCTX: VCOperationCTX,
        template: TemplateData
    ) {
        if (template.vcName != null) {
            vcNameManager.performVCRename(vcOperationCTX, template.vcName!!)
        }

        if (template.vcLimit != null) {
            vcOperationCTX.temporaryVCManager.setUserLimit(template.vcLimit!!)
            vcOperationCTX.markTemporaryVCManagerAsUpdated()
        }

        if (template.vcBitrate != null) {
            vcOperationCTX.temporaryVCManager.setBitrate(template.vcBitrate!!)
            vcOperationCTX.markTemporaryVCManagerAsUpdated()
        }

        if (template.vcRegion != null) {
            vcOperationCTX.temporaryVCManager.setRegion(Region.fromKey(template.vcRegion))
            vcOperationCTX.markTemporaryVCManagerAsUpdated()
        }
    }
}