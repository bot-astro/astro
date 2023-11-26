package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import org.springframework.stereotype.Component
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.InitialPosition
import space.astro.shared.core.models.database.TemporaryVCData

@Component
class VCPositionManager {
    companion object {
        /**
         * Minimum distance between two temporary VCs
         * Set to two because it needs to leave a space for waiting rooms
         */
        private const val RAW_POSITIONS_MINIMUM_DISTANCE = 2
        const val RAW_POSITIONS_WAITING_ROOM_DISTANCE = 1
    }

    private fun getRawPositionWithoutPositionalData(
        generator: GeneratorData,
        generatorVC: VoiceChannel
    ): Int? {
        // If the temporary VC gets generated in a different category than the one of the generator
        // then no position gets calculated
        if (generator.category != generatorVC.parentCategoryId) {
            return null
        }

        val generatorPosition = generatorVC.positionRaw

        return when (generator.initialPosition) {
            InitialPosition.BOTTOM -> null
            InitialPosition.BEFORE -> {
                generatorPosition - RAW_POSITIONS_MINIMUM_DISTANCE
            }
            InitialPosition.AFTER -> {
                generatorPosition + RAW_POSITIONS_MINIMUM_DISTANCE
            }
        }?.coerceAtLeast(0)
    }

    private fun getRawPositionWithIncrementalPosition(
        incrementalPosition: Int,
        generator: GeneratorData,
        generatorVC: VoiceChannel
    ): Int {
        val defaultRawPosition = getRawPositionWithoutPositionalData(generator, generatorVC)
            ?: 0

        return defaultRawPosition + (RAW_POSITIONS_MINIMUM_DISTANCE * incrementalPosition)
    }

    /**
     * Gets the first available incremental position for a specific generator
     *
     * @param generatorId id of the generator
     * @param excludedVCId id of a channel to exclude from the search, usually the existing channel of which we need to recalculate the position
     * @param temporaryVCs list of existing temporary VCs
     */
    fun getIncrementalPosition(generatorId: String, excludedVCId: String?, temporaryVCs: List<TemporaryVCData>): Int {
        var incrementalPosition = 1

        val positions = temporaryVCs.filter {
            it.generatorId == generatorId
                    && it.id != excludedVCId
                    && it.incrementalPosition != null
        }
            .mapNotNull { it.incrementalPosition }
            .toSortedSet()

        for (position in positions) {
            if (incrementalPosition != position)
                break
            incrementalPosition++
        }

        return incrementalPosition
    }

    /**
     * Gets the raw discord position of a temporary voice channel
     * based on the [incrementalPosition]
     *
     * When [incrementalPosition] is null it will calculate the best position based on the [generator] data
     * otherwise it will search for the correct position based on the other temporary VCs
     * which are supposed to use incremental ordering already
     */
    fun getRawPosition(incrementalPosition: Int?, generator: GeneratorData, generatorVC: VoiceChannel): Int? {
        return if (incrementalPosition == null) {
            getRawPositionWithoutPositionalData(generator, generatorVC)
        } else {
            getRawPositionWithIncrementalPosition(incrementalPosition, generator, generatorVC)
        }
    }
}