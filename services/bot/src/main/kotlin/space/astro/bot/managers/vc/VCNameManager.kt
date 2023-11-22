package space.astro.bot.managers.vc

import org.springframework.stereotype.Component
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.managers.util.PremiumRequirementDetector
import space.astro.bot.managers.vc.ctx.VCOperationCTX
import space.astro.bot.models.error.ConfigurationErrors
import space.astro.shared.core.models.database.InitialPosition
import space.astro.shared.core.models.database.TemporaryVCData

@Component
class VCNameManager(
    val premiumRequirementDetector: PremiumRequirementDetector,
    val vcPositionManager: VCPositionManager
) {
    /**
     * Change the name of a temporary vc
     *
     * @param newNameTemplate
     * @throws ConfigurationException
     */
    fun performVCRename(
        vcOperationCTX: VCOperationCTX,
        newNameTemplate: String
    ) {
        vcOperationCTX.apply {
            if (!temporaryVCData.canBeRenamed()) {
                return
            }

            if (!shouldRenameBasedOnRenameConditions()) {
                return
            }

            validatePremiumRequirements(newNameTemplate)

            performPositionUpdates(newNameTemplate)

            val newName = VariablesManager.computeVcNameForExisting(
                template = newNameTemplate,
                owner = temporaryVCOwner,
                temporaryVC = temporaryVC,
                incrementalPosition = temporaryVCData.incrementalPosition
            )

            performNameUpdates(newName)
        }
    }

    /**
     * Refresh the name of a temporary vc
     *
     * @throws ConfigurationException
     */
    fun performVCNameRefresh(vcOperationCTX: VCOperationCTX) {
        vcOperationCTX.apply {
            if (!temporaryVCData.canBeRenamed()) {
                return
            }

            if (!shouldRenameBasedOnRenameConditions()) {
                return
            }

            val nameTemplate = VariablesManager.getNameTemplateForRefresh(
                temporaryVCData = temporaryVCData,
                generatorData = generatorData
            )

            validatePremiumRequirements(nameTemplate)

            performPositionUpdates(nameTemplate)

            val newName = VariablesManager.computeVcNameForExisting(
                template = nameTemplate,
                owner = temporaryVCOwner,
                temporaryVC = temporaryVC,
                incrementalPosition = temporaryVCData.incrementalPosition
            )
            performNameUpdates(newName)
        }
    }
    
    
    /////////////////////////////////
    /// TEMPORARY VC DATA HELPERS ///
    /////////////////////////////////
    
    private fun TemporaryVCData.canBeRenamed(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (lastNameChange == null || (currentTime - lastNameChange!!) > 600000)
            return true

        if (nameChanges < 2)
            return true

        return false
    }

    private fun TemporaryVCData.performRenameOperationsOnTemporaryVCData(
        renamedByUser: Boolean
    ) {
        if (canBeRenamed()) {
            val currentTime = System.currentTimeMillis()

            if (lastNameChange == null || (currentTime - lastNameChange!!) > 600000) {
                lastNameChange = currentTime
                nameChanges = 1
            } else {
                lastNameChange = currentTime
                nameChanges++
            }

            if (renamedByUser) {
                this.renamed = true
            }
        }
    }

    //////////////////
    /// VALIDATORS ///
    //////////////////

    /**
     * Checks whether premium variables can be used depending on the guild premium status
     *
     * @throws ConfigurationException if premium variables have been detected and the guild is not premium
     */
    private fun VCOperationCTX.validatePremiumRequirements(nameTemplate: String) {
        if (premiumRequirementDetector.canUseVCNameTemplate(guildData, nameTemplate)) {
            throw ConfigurationException(ConfigurationErrors.premiumVariables())
        }
    }

    /////////////////////
    /// OTHER HELPERS ///
    /////////////////////

    /**
     * Checks whether the VC name should be renamed based on the generator rename conditions
     *
     * @return true if it should be renamed, false otherwise
     */
    private fun VCOperationCTX.shouldRenameBasedOnRenameConditions(): Boolean {
        val renameConditions = generatorData.renameConditions

        if (vcOperationOrigin == VCOperationCTX.VCOperationOrigin.USER_RENAME)
            return true

        if (!generatorData.renameConditions.renamed && temporaryVCData.renamed)
            return false

        return when (vcOperationOrigin) {
            VCOperationCTX.VCOperationOrigin.ACTIVITY_CHANGE -> renameConditions.activityChange
            VCOperationCTX.VCOperationOrigin.STATE_CHANGE -> renameConditions.stateChange
            VCOperationCTX.VCOperationOrigin.OWNER_CHANGE -> renameConditions.ownerChange
            else -> true
        }
    }

    /**
     * Updates the incremental, raw and waiting room raw position related to this temporary vc
     *
     * @param nameTemplate the name template of the temporary vc to check whether positional data is required
     */
    private fun VCOperationCTX.performPositionUpdates(
        nameTemplate: String
    ) {
        val requiresPositionalData = VariablesManager.doesTemplateRequireVCPositionalData(nameTemplate)

        if (requiresPositionalData) {
            val incrementalPosition = vcPositionManager.getIncrementalPosition(
                generatorId = generatorData.id,
                excludedVCId = null,
                temporaryVCs = temporaryVCsData
            )
            temporaryVCData.incrementalPosition = incrementalPosition

            vcPositionManager.getRawPosition(
                incrementalPosition = incrementalPosition,
                generator = generatorData,
                generatorVC = generator
            )
                ?.also {  temporaryVCRawPosition ->
                    temporaryVCManager.setPosition(temporaryVCRawPosition)
                    markTemporaryVCManagerAsUpdated()

                    if (waitingRoomManager != null) {
                        when (generatorData.waitingPosition) {
                            InitialPosition.AFTER -> temporaryVCRawPosition + 1
                            InitialPosition.BEFORE -> temporaryVCRawPosition - 1
                            InitialPosition.BOTTOM -> null
                        }?.also {  waitingRoomRawPosition ->
                            waitingRoomManager.setPosition(waitingRoomRawPosition)
                            markWaitingRoomManagerAsUpdated()
                        }
                    }
                }
        }
    }

    private fun VCOperationCTX.performNameUpdates(
        name: String
    ) {
        if (temporaryVC.name == name) {
            return
        }

        temporaryVCData.performRenameOperationsOnTemporaryVCData(
            renamedByUser = vcOperationOrigin == VCOperationCTX.VCOperationOrigin.USER_RENAME
        )
        temporaryVCManager.setName(name)
        markTemporaryVCManagerAsUpdated()
    }
}