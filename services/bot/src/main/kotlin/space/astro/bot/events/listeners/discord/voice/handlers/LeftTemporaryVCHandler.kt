package space.astro.bot.events.listeners.discord.voice.handlers

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class LeftTemporaryVCHandler(
    private val temporaryVCRepository: TemporaryVCRepository,
    private val vcOwnershipManager: VCOwnershipManager,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) {
    suspend fun handle(
        event: VCEvent.LeftTemporaryVC,
        memberRolesManager: SimpleMemberRolesManager,
    ) {
        val data = event.vcEventData
        val guild = data.guild
        val entity = event.temporaryVCEntity

        val vc =
            data.leftChannel?.asVoiceChannel()
                ?: throw IllegalStateException("Received left temporary VC event with a null left channel")
        val privateChat = entity.chatID?.let { guild.getTextChannelById(it) }
        val waitingRoom = entity.waitingID?.let { guild.getVoiceChannelById(it) }
        val newOwner = vc.members.firstOrNull { !it.user.isBot }
        val locale = data.guildSettings.supportedLocale

        if (newOwner == null) {
            val reason = L.t(locale, "reason.vc.deleted_empty", "count" to vc.members.size)

            temporaryVCRepository.delete(guild.id, entity.id)

            vc.delete().reason(reason).queue()
            privateChat?.delete()?.reason(reason)?.queue()
            waitingRoom?.delete()?.reason(reason)?.queue()

            return
        }

        if (event.ownerLeft) {
            changeOwner(event, newOwner, vc, privateChat, waitingRoom, memberRolesManager)
        } else {
            val memberId = data.member.idLong
            val reason = L.t(locale, "reason.vc.member_left")

            vc.manager.removePermissionOverride(memberId).reason(reason).queue()
            privateChat?.manager?.removePermissionOverride(memberId)?.reason(reason)?.queue()
            waitingRoom?.manager?.removePermissionOverride(memberId)?.reason(reason)?.queue()
        }

        if (entity.chatLogs) {
            val logChat: GuildMessageChannel = privateChat ?: vc
            logChat.sendVCLog(data.guildSettings.supportedLocale, "vc.log.left", "user" to data.member.asMention)
        }
    }

    private suspend fun changeOwner(
        event: VCEvent.LeftTemporaryVC,
        newOwner: Member,
        vc: VoiceChannel,
        privateChat: TextChannel?,
        waitingRoom: VoiceChannel?,
        memberRolesManager: SimpleMemberRolesManager,
    ) {
        val data = event.vcEventData
        val guild = data.guild
        val generatorSettings = data.generators.firstOrNull { it.id == event.temporaryVCEntity.generatorID }
        val generator = guild.getVoiceChannelById(event.temporaryVCEntity.generatorID)

        if (generatorSettings == null || generator == null) return

        val ctx =
            VCOperationCTX(
                guildSettings = data.guildSettings,
                generator = generator,
                generatorSettings = generatorSettings,
                temporaryVCOwner = data.member,
                temporaryVC = vc,
                temporaryVCManager = vc.manager,
                temporaryVCEntity = event.temporaryVCEntity,
                temporaryVCEntities = data.temporaryVCEntities,
                privateChat = privateChat,
                privateChatManager = privateChat?.manager,
                waitingRoom = waitingRoom,
                waitingRoomManager = waitingRoom?.manager,
                vcOperationOrigin = VCOperationCTX.VCOperationOrigin.OWNER_CHANGE,
            )

        // The old owner role is removed through the roles manager (and not VCOwnershipManager.handleOwnerRoleMigration)
        // because the manager would otherwise re-apply the cached roles of the member that just left
        val ownerRole = generatorSettings.ownerRole?.let { guild.getRoleById(it) }
        ownerRole?.also { memberRolesManager.remove(it) }

        vcOwnershipManager.changeOwner(ctx, newOwner)

        ctx.queueUpdatedManagers(
            failure = { managerType, e ->
                configurationErrorEventPublisher.publishConfigurationErrorEvent(
                    ConfigurationErrorEntity(
                        guildId = guild.id,
                        description = "An unknown error occurred while updating the ${managerType.readableName}: ${e.message ?: ""}",
                    ),
                )
            },
        )
        temporaryVCRepository.save(guild.id, ctx.temporaryVCEntity)

        ownerRole?.also {
            guild
                .addRoleToMember(newOwner, it)
                .reason(L.t(data.guildSettings.supportedLocale, "reason.vc.owner_role_added"))
                .queue()
        }
    }
}
