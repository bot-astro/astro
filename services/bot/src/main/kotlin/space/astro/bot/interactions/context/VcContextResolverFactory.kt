package space.astro.bot.interactions.context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.InteractionException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class VcContextResolverFactory(
    private val temporaryVCRepository: TemporaryVCRepository,
) {
    fun create(
        ownershipRequired: Boolean,
        origin: VCOperationCTX.VCOperationOrigin,
    ): ContextResolver<VcInteractionContext> =
        ContextResolver { _: GenericInteractionCreateEvent, base ->
            VcInteractionContext(resolve(base, ownershipRequired, origin), base)
        }

    private suspend fun resolve(
        base: InteractionContext,
        ownershipRequired: Boolean,
        origin: VCOperationCTX.VCOperationOrigin,
    ): VCOperationCTX {
        val guild = base.guild
        val member = base.member

        val vc =
            member.voiceState
                ?.channel
                ?.takeIf { it.type == ChannelType.VOICE }
                ?.asVoiceChannel()
                ?: throw InteractionException("You need to be in a VC to use this command!")

        val guildSettings =
            base.guildSettings
                ?: throw InteractionException("Astro is not configured in this server!")

        val temporaryVCEntities = withContext(Dispatchers.IO) { temporaryVCRepository.getOfGuild(guild.id) }
        val temporaryVCEntity =
            temporaryVCEntities.firstOrNull { it.id == vc.id }
                ?: throw InteractionException("You must be in a temporary VC to use this!")

        if (ownershipRequired && temporaryVCEntity.ownerID != member.id) {
            throw InteractionException("You need to be the owner of the temporary VC to use this!")
        }

        val generatorSettings = guildSettings.generators.firstOrNull { it.id == temporaryVCEntity.generatorID }
        val generator = generatorSettings?.id?.let { guild.getVoiceChannelById(it) }

        if (generatorSettings == null || generator == null) {
            throw InteractionException("The generator of this temporary VC has been deleted!")
        }

        val privateChat = temporaryVCEntity.chatID?.let { guild.getTextChannelById(it) }
        val waitingRoom = temporaryVCEntity.waitingID?.let { guild.getVoiceChannelById(it) }

        return VCOperationCTX(
            guildSettings = guildSettings,
            generator = generator,
            generatorSettings = generatorSettings,
            temporaryVCOwner = guild.getMemberById(temporaryVCEntity.ownerID),
            temporaryVC = vc,
            temporaryVCManager = vc.manager,
            temporaryVCEntity = temporaryVCEntity,
            temporaryVCEntities = temporaryVCEntities,
            privateChat = privateChat,
            privateChatManager = privateChat?.manager,
            waitingRoom = waitingRoom,
            waitingRoomManager = waitingRoom?.manager,
            vcOperationOrigin = origin,
        )
    }
}
