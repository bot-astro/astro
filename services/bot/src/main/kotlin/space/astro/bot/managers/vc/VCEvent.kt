package space.astro.bot.managers.vc

import space.astro.shared.core.models.database.GeneratorData

sealed class VCEvent(
    val vcEventData: VCEventData
) {
    class JoinedGenerator(
        vcEventData: VCEventData,
        val generatorData: GeneratorData
    ): VCEvent(vcEventData)
    class JoinedTemporaryVC(
        vcEventData: VCEventData
    ): VCEvent(vcEventData)
    class JoinedConnectedVC(
        vcEventData: VCEventData
    ): VCEvent(vcEventData)
    class OwnerLeftTemporaryVC(
        vcEventData: VCEventData
    ): VCEvent(vcEventData)
    class LeftTemporaryVC(
        vcEventData: VCEventData
    ): VCEvent(vcEventData)
    class LeftConnectedVC(
        vcEventData: VCEventData
    ): VCEvent(vcEventData)
}