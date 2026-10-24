package space.astro.bot.events.listeners.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCNameManager
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.interactions.InteractionException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.ConfigurationErrorCode
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

private val log = KotlinLogging.logger { }

@Component
class MemberUpdateActivitiesEventListener(
    private val temporaryVCRepository: TemporaryVCRepository,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val vcNameManager: VCNameManager,
    private val vcPrivateChatManager: VCPrivateChatManager,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    private val premiumManager: PremiumManager,
) {
    // IO because the repositories are blocking
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun onUserUpdateActivities(event: UserUpdateActivitiesEvent) {
        if (event.user.isBot) {
            return
        }

        // Cheap checks on the cache before hitting the databases
        val vc =
            event.member.voiceState
                ?.channel
                ?.takeIf { it.type == ChannelType.VOICE }
                ?.asVoiceChannel()
                ?: return

        scope.launch {
            try {
                handle(event, vc)
            } catch (e: Exception) {
                log.error(e) { "Unhandled exception while handling an activity update in guild ${event.guild.id}" }
            }
        }
    }

    private fun handle(
        event: UserUpdateActivitiesEvent,
        vc: VoiceChannel,
    ) {
        val guild = event.guild
        val member = event.member

        val temporaryVCEntities = temporaryVCRepository.getOfGuild(guild.id)
        val temporaryVCEntity =
            temporaryVCEntities
                .firstOrNull { it.id == vc.id }
                ?.takeIf { it.ownerID == member.id }
                ?: return

        val guildSettings = guildSettingsRepository
                .findByGuildID(guild.id)
                ?.takeIf { premiumManager.isGuildPremium(it) }
                ?: return

        val generatorSettings =
            guildSettings.generators
                .firstOrNull { it.id == temporaryVCEntity.generatorID }
                ?.takeIf {
                    it.renameConditions.activityChange
                }
                ?: return

        val generator = guild.getVoiceChannelById(generatorSettings.id) ?: return
        val privateChat = temporaryVCEntity.chatID?.let { guild.getTextChannelById(it) }
        val waitingRoom = temporaryVCEntity.waitingID?.let { guild.getVoiceChannelById(it) }

        val ctx =
            VCOperationCTX(
                guildSettings = guildSettings,
                generator = generator,
                generatorSettings = generatorSettings,
                temporaryVCOwner = member,
                temporaryVC = vc,
                temporaryVCManager = vc.manager,
                temporaryVCEntity = temporaryVCEntity,
                temporaryVCEntities = temporaryVCEntities,
                privateChat = privateChat,
                privateChatManager = privateChat?.manager,
                waitingRoom = waitingRoom,
                waitingRoomManager = waitingRoom?.manager,
                vcOperationOrigin = VCOperationCTX.VCOperationOrigin.ACTIVITY_CHANGE,
            )

        try {
            vcNameManager.performVCNameRefresh(ctx)
            vcPrivateChatManager.performPrivateChatNameRefresh(ctx)
            vcWaitingRoomManager.performWaitingRoomNameRefresh(ctx)
        } catch (e: ConfigurationException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(e.configurationErrorEntity)
        } catch (_: InteractionException) {
            // the vc cannot be renamed right now (e.g. rename cooldown), nothing to report
        }

        ctx.queueUpdatedManagers(
            failure = { managerType, e ->
                configurationErrorEventPublisher.publishConfigurationErrorEvent(
                    ConfigurationErrorEntity(
                        guildId = guild.id,
                        code = ConfigurationErrorCode.UPDATE_MANAGER_FAILED,
                        args = mapOf("manager" to managerType.readableName, "message" to (e.message ?: "")),
                    ),
                )
            },
        )

        temporaryVCRepository.save(guild.id, ctx.temporaryVCEntity)
    }
}
