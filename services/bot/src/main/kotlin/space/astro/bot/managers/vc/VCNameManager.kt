package space.astro.bot.managers.vc

import space.astro.bot.managers.vc.ctx.VCOperationCTX
import space.astro.shared.core.models.database.InitialPosition
import space.astro.shared.core.models.database.TemporaryVCData

object VCNameManager {
    /**
     * Change the name of a temporary vc
     *
     * @param newNameTemplate
     */
    fun VCOperationCTX.performVCRename(newNameTemplate: String) {
        if (!temporaryVCData.canBeRenamed()) {
            return
        }

        performPositionUpdates(newNameTemplate)

        val newName = VariablesManager.computeVcNameForExisting(
            template = newNameTemplate,
            owner = temporaryVCOwner,
            temporaryVC = temporaryVC,
            incrementalPosition = temporaryVCData.incrementalPosition
        )
        
        performNameUpdates(newName)
    }

    /**
     * Refresh the name of a temporary vc
     */
    fun VCOperationCTX.performVCNameRefresh() {
        if (!temporaryVCData.canBeRenamed()) {
            return
        }
        
        val nameTemplate = VariablesManager.getNameTemplateForRefresh(temporaryVCData, generatorData)
        
        performPositionUpdates(nameTemplate)
        
        val newName = VariablesManager.computeVcNameForExisting(
            template = nameTemplate,
            owner = temporaryVCOwner,
            temporaryVC = temporaryVC,
            incrementalPosition = temporaryVCData.incrementalPosition
        )
        performNameUpdates(newName)
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

    private fun TemporaryVCData.performRenameOperationsOnTemporaryVCData() {
        if (canBeRenamed()) {
            val currentTime = System.currentTimeMillis()

            if (lastNameChange == null || (currentTime - lastNameChange!!) > 600000) {
                lastNameChange = currentTime
                nameChanges = 1
            } else {
                lastNameChange = currentTime
                nameChanges++
            }
        }
    }
    
    /////////////////////
    /// OTHER HELPERS ///
    /////////////////////

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
            val incrementalPosition = VCPositionManager.getIncrementalPosition(
                generatorId = generatorData.id,
                excludedVCId = null,
                temporaryVCs = temporaryVCsData
            )
            temporaryVCData.incrementalPosition = incrementalPosition

            VCPositionManager.getRawPosition(
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

        temporaryVCData.performRenameOperationsOnTemporaryVCData()
        temporaryVCManager.setName(name)
        markTemporaryVCManagerAsUpdated()
    }
}