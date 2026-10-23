package space.astro.bot.models.discord.vc.event

import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.models.database.guildSettings.ConnectionData
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings

sealed class VCEvent(
    val vcEventData: VCEventData,
) {
    class JoinedGenerator(
        vcEventData: VCEventData,
        val generatorSettings: GeneratorSettings,
    ) : VCEvent(vcEventData)

    class JoinedTemporaryVC(
        vcEventData: VCEventData,
        val temporaryVCEntity: TemporaryVCEntity,
    ) : VCEvent(vcEventData)

    class JoinedConnectedVC(
        vcEventData: VCEventData,
        val connectionData: ConnectionData,
    ) : VCEvent(vcEventData)

    class LeftTemporaryVC(
        vcEventData: VCEventData,
        val ownerLeft: Boolean,
        val temporaryVCEntity: TemporaryVCEntity,
    ) : VCEvent(vcEventData)

    class LeftConnectedVC(
        vcEventData: VCEventData,
        val connectionData: ConnectionData,
    ) : VCEvent(vcEventData)
}
