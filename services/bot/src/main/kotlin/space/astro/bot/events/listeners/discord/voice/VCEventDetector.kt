package space.astro.bot.events.listeners.discord.voice

import net.dv8tion.jda.api.entities.channel.ChannelType
import org.springframework.stereotype.Component
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.bot.models.discord.vc.event.VCEventData
import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.models.database.guildSettings.ConnectionData

@Component
class VCEventDetector {
    /**
     * Detects any event related to Astro features that got triggered by a user moving between voice channels
     *
     * @param vcEventData all the necessary data about the voice event
     * @throws IllegalStateException when [vcEventData] has neither a [VCEventData.joinedChannel] nor a [VCEventData.leftChannel]
     * @return a [List] of [VCEvent]s
     */
    fun detectAstroVCEvents(vcEventData: VCEventData): List<VCEvent> {
        val events = mutableListOf<VCEvent>()

        check(vcEventData.joinedChannel != null || vcEventData.leftChannel != null) {
            "Received invalid astro voice event data: missing both joined and left audio channel"
        }

        // These variables are needed for optimizing connection events calculation
        var joinedTemporaryVC: TemporaryVCEntity? = null
        var leftTemporaryVC: TemporaryVCEntity? = null
        var joinedConnection: ConnectionData? = null
        var leftConnection: ConnectionData? = null

        // temporary vc join / generator join
        vcEventData.joinedChannel
            ?.takeIf { it.type == ChannelType.VOICE }
            ?.also { joinedVC ->
                val joinedGenerator = vcEventData.generators.firstOrNull { it.id == joinedVC.id }

                if (joinedGenerator != null) {
                    events.add(VCEvent.JoinedGenerator(vcEventData, joinedGenerator))
                } else {
                    vcEventData.temporaryVCEntities
                        .firstOrNull { it.id == joinedVC.id }
                        ?.also {
                            joinedTemporaryVC = it
                            events.add(VCEvent.JoinedTemporaryVC(vcEventData, it))
                        }
                }
            }

        // temporary vc left
        vcEventData.leftChannel
            ?.takeIf { it.type == ChannelType.VOICE }
            ?.also { leftVC ->
                vcEventData.temporaryVCEntities
                    .firstOrNull { it.id == leftVC.id }
                    ?.also { temporaryVCEntity ->
                        leftTemporaryVC = temporaryVCEntity

                        events.add(
                            VCEvent.LeftTemporaryVC(
                                vcEventData = vcEventData,
                                ownerLeft = temporaryVCEntity.ownerID == vcEventData.userId,
                                temporaryVCEntity = temporaryVCEntity,
                            ),
                        )
                    }
            }

        /*
            Conditions to accept a connection as joined:
            - Joined channel is connected (normal or generator doesn't matter)
            - Joined channel is under connected category
            - Joined channel is a temporary VC generated from a connected generator
         */
        vcEventData.joinedChannel?.also { joinedVC ->
            joinedConnection =
                vcEventData.connections.firstOrNull { connection ->
                    val isConnected = connection.id == joinedVC.id
                    val isNotGenerator = vcEventData.generators.none { gen -> gen.id == joinedVC.id }
                    val parentIsConnected = connection.id == joinedVC.parentCategoryId
                    val generatorOfJoinedTemporaryVCIsConnected = connection.id == joinedTemporaryVC?.generatorID

                    isConnected || (isNotGenerator && (parentIsConnected || generatorOfJoinedTemporaryVCIsConnected))
                }
        }

        /*
            Conditions to accept a connection as left:
            - Left channel is connected and isn't a generator
            - Left channel is under a connected category and left channel isn't a generator
            - Left channel is a temporary VC generated from a connected generator
         */
        vcEventData.leftChannel
            ?.takeIf { vcEventData.generators.none { gen -> gen.id == it.id } }
            ?.also { leftVC ->
                leftConnection =
                    vcEventData.connections.firstOrNull { connection ->
                        val isConnected = connection.id == leftVC.id
                        val parentIsConnected = connection.id == leftVC.parentCategoryId
                        val generatorOfLeftTemporaryVCIsConnected = connection.id == leftTemporaryVC?.generatorID
                        val hasNotJoinedTemporaryVCOfConnectedGenerator = connection.id != joinedTemporaryVC?.generatorID

                        (isConnected || parentIsConnected || generatorOfLeftTemporaryVCIsConnected) &&
                            hasNotJoinedTemporaryVCOfConnectedGenerator
                    }
            }

        val joined = joinedConnection
        val left = leftConnection

        // moving between channels of the same connection (or of connections sharing the role) changes nothing
        val sameConnection = joined != null && left != null && (joined.id == left.id || joined.roleID == left.roleID)

        if (joined != null && !sameConnection) {
            events.add(VCEvent.JoinedConnectedVC(vcEventData, joined))
        }

        if (left != null && !sameConnection && !left.permanent) {
            events.add(VCEvent.LeftConnectedVC(vcEventData, left))
        }

        return events
    }
}
