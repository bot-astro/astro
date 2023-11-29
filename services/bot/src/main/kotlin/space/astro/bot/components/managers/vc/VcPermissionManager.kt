package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.models.database.PermissionsInherited
import space.astro.shared.core.models.database.VCState

@Component
class VcPermissionManager(
    private val vcNameManager: VCNameManager,
    private val configurationErrorService: ConfigurationErrorService
) {
    /**
     * @throws InsufficientPermissionException
     * @throws ConfigurationException
     */
    fun changeState(
        vcOperationCTX: VCOperationCTX,
        newState: VCState
    ) {
        if (newState == vcOperationCTX.temporaryVCData.state) {
            return
        }

        val targetRole = if (vcOperationCTX.generatorData.permissionsTargetRole != null) {
            vcOperationCTX.guild.getRoleById(vcOperationCTX.generatorData.permissionsTargetRole!!)
                ?: throw ConfigurationException(configurationErrorService.missingGeneratorTargetRole(vcOperationCTX.generator.name))
        } else {
            vcOperationCTX.guild.publicRole
        }

        vcOperationCTX.temporaryVCData.state = newState

        if (newState.permissionDenied != null) {
            vcOperationCTX.temporaryVCManager.modifyPermissionOverride(
                targetRole,
                0,
                newState.permissionDenied!!.rawValue
            )
        }

        if (newState.permissionReset != null) {
            val permissionOverrideInheritedForTargetRole = vcOperationCTX.calculateInheritedPermissions()
                .firstOrNull { it.id == targetRole.id }

            if (permissionOverrideInheritedForTargetRole != null) {
                val allowed = permissionOverrideInheritedForTargetRole.allowed.apply { remove(newState.permissionReset) }
                val denied = permissionOverrideInheritedForTargetRole.denied.apply { remove(newState.permissionReset) }

                vcOperationCTX.temporaryVCManager.modifyPermissionOverride(
                    targetRole,
                    allowed,
                    denied
                )
            }
        }

        vcOperationCTX.markTemporaryVCManagerAsUpdated()
        vcNameManager.performVCNameRefresh(vcOperationCTX)
    }

    private fun VCOperationCTX.calculateInheritedPermissions(): List<PermissionOverride> {
        return when (generatorData.permissionsInherited) {
            PermissionsInherited.NONE -> {
                emptyList()
            }
            PermissionsInherited.GENERATOR -> {
                generator.permissionOverrides
            }
            PermissionsInherited.CATEGORY -> {
                temporaryVC.parentCategory?.permissionOverrides ?: emptyList()
            }
        }
    }
}