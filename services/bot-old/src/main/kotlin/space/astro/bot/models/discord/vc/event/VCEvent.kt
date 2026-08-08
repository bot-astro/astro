package space.astro.bot.models.discord.vc.event

import space.astro.shared.core.models.database.ConnectionData
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.TemporaryVCData

sealed class VCEvent(
    val vcEventData: VCEventData
) {
    class JoinedGenerator(
        vcEventData: VCEventData,
        val generatorData: GeneratorData
    ): VCEvent(vcEventData)
    class JoinedTemporaryVC(
        vcEventData: VCEventData,
        val temporaryVCData: TemporaryVCData
    ): VCEvent(vcEventData)

    class JoinedConnectedVC(
        vcEventData: VCEventData,
        val connectionData: ConnectionData
    ): VCEvent(vcEventData)

    class LeftTemporaryVC(
        vcEventData: VCEventData,
        val ownerLeft: Boolean,
        val temporaryVCData: TemporaryVCData,
    ): VCEvent(vcEventData)

    class LeftConnectedVC(
        vcEventData: VCEventData,
        val connectionData: ConnectionData
    ): VCEvent(vcEventData)
}