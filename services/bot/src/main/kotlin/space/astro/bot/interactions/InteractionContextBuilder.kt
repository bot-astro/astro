package space.astro.bot.interactions

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import org.springframework.stereotype.Component
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.models.database.GuildData

@Component
class InteractionContextBuilder(
    private val temporaryVCDao: TemporaryVCDao
) {

    /**
     * @throws InteractionContextBuilderException if something fails
     */
    fun buildVcInteractionContext(
        interactionCreateEvent: GenericInteractionCreateEvent,
        vcInteractionContextInfo: VcInteractionContextInfo,
        guildData: GuildData,
        usedInterfaceComponent: Boolean
    ) : VcInteractionContext {
        val guild = interactionCreateEvent.guild
            ?: throw IllegalStateException("Received a interaction create event not from a guild in a vc context interaction")
        val member = interactionCreateEvent.member
            ?: throw IllegalStateException("Received a interaction create event with a null member (not from a guild) in a vc context interaction")

        val vc = member.voiceState!!
            .channel
            ?.takeIf { it.type == ChannelType.VOICE }
            ?.asVoiceChannel()
            ?: throw InteractionContextBuilderException(Embeds.error("You need to be in a VC to use this command!"))

        val temporaryVCsData = temporaryVCDao.getAll(guild.id)
        val temporaryVCData = temporaryVCsData.firstOrNull { it.id == vc.id }
            ?: throw InteractionContextBuilderException(Embeds.error("You must be in a temporary VC to use this button!"))

        if (vcInteractionContextInfo.ownershipRequired) {
            if (temporaryVCData.ownerId != member.id) {
                throw InteractionContextBuilderException(Embeds.error("You need to be the owner of the temporary VC to use this button!"))
            }
        }

        val generatorData = guildData.generators
            .firstOrNull { it.id == temporaryVCData.generatorId }

        val generator = generatorData?.id?.let { guild.getVoiceChannelById(it) }

        if (generatorData == null || generator == null) {
            throw InteractionContextBuilderException(Embeds.error("The generator of this temporary VC has been deleted!"))
        }

        val privateChat = temporaryVCData.chatID?.let { guild.getTextChannelById(it) }
        val waitingRoom = temporaryVCData.waitingID?.let { guild.getVoiceChannelById(it) }

        val vcOperationCTX = VCOperationCTX(
            guildData = guildData,
            generator = generator,
            generatorData = generatorData,
            temporaryVCOwner = guild.getMemberById(temporaryVCData.ownerId),
            temporaryVC = vc,
            temporaryVCManager = vc.manager,
            temporaryVCData = temporaryVCData,
            temporaryVCsData = temporaryVCsData,
            privateChat = privateChat,
            privateChatManager = privateChat?.manager,
            waitingRoom = waitingRoom,
            waitingRoomManager = waitingRoom?.manager,
            vcOperationOrigin = vcInteractionContextInfo.vcOperationOrigin
        )

        return VcInteractionContext(
            vcOperationCTX = vcOperationCTX,
            guild = guild,
            member = member,
            user = interactionCreateEvent.user,
            usedInterfaceComponent = usedInterfaceComponent
        )
    }
}