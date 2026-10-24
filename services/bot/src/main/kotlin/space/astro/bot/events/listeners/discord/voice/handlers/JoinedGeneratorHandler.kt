package space.astro.bot.events.listeners.discord.voice.handlers

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.stereotype.Component
import space.astro.bot.components.InterfaceManager
import space.astro.bot.components.managers.vc.VCPositionManager
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
import space.astro.bot.components.managers.vc.VariablesManager
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.exceptions.PremiumConfigurationErrors
import space.astro.bot.interactions.InteractionCooldownService
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.bot.ui.Containers
import space.astro.bot.utils.extensions.modifyPermissionOverride
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.ConfigurationErrorCode
import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Handles the creation of temporary voice channels via generators
 */
@Component
class JoinedGeneratorHandler(
    private val temporaryVCRepository: TemporaryVCRepository,
    private val interactionCooldownService: InteractionCooldownService,
    private val vcPositionManager: VCPositionManager,
    private val vcPrivateChatManager: VCPrivateChatManager,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
    private val interfaceManager: InterfaceManager,
    private val premiumManager: PremiumManager,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) {
    companion object {
        private val GENERATOR_COOLDOWN = 3.seconds
        private const val GENERATOR_COOLDOWN_KEY = "generator"
        private const val MAX_CATEGORY_CHANNELS = 50
    }

    /**
     * @return true if a temporary vc has been created
     *
     * @throws ConfigurationException
     */
    suspend fun handle(
        event: VCEvent.JoinedGenerator,
        memberRolesManager: SimpleMemberRolesManager,
    ): Boolean {
        val data = event.vcEventData
        val guild = data.guild
        val owner = data.member
        val settings = event.generatorSettings
        val guildSettings = data.guildSettings
        val locale = guildSettings.supportedLocale

        val generatorVC =
            data.joinedChannel?.asVoiceChannel()
                ?: throw IllegalStateException("Received joined generator event with a null joined channel")

        if (premiumManager.exceededMaximumGeneratorAmount(guildSettings)) {
            throw PremiumConfigurationErrors.maximumAmountOfGenerators(guild.id)
        }

        //////////////
        /// QUEUES ///
        //////////////
        if (settings.queueMode) {
            val generatorTemporaryVCIds =
                data.temporaryVCEntities
                    .filter { it.generatorID == settings.id }
                    .map { it.id }
                    .toSet()

            val availableTemporaryVC =
                guild.voiceChannelCache.firstOrNull {
                    it.id in generatorTemporaryVCIds &&
                        (it.userLimit == 0 || it.members.size < it.userLimit) &&
                        owner.hasPermission(it, Permission.VOICE_CONNECT)
                }

            if (availableTemporaryVC != null) {
                guild.moveVoiceMember(owner, availableTemporaryVC).queue()
                return false
            }
        }

        /////////////////////////////////////////
        /// CATEGORY CALCULATION AND FALLBACK ///
        /////////////////////////////////////////
        val category = settings.category?.let { guild.getCategoryById(it) }

        if (category != null && category.channels.size >= MAX_CATEGORY_CHANNELS) {
            val fallbackGenerator =
                settings.fallbackId
                    ?.let { fallback -> data.generators.firstOrNull { it.id == fallback && it.id != data.leftChannel?.id } }
                    ?.let { guild.getVoiceChannelById(it.id) }
                    ?.takeIf { it.parentCategoryId != category.id }
                    ?: throw ConfigurationException(
                        ConfigurationErrorEntity(
                            guildId = guild.id,
                            code = ConfigurationErrorCode.GENERATOR_CATEGORY_FULL,
                            args = mapOf("generator" to generatorVC.name),
                        ),
                    )

            if (!premiumManager.canUseFallbackGenerator(guildSettings)) {
                throw PremiumConfigurationErrors.fallbackGenerator(guild.id)
            }

            guild
                .moveVoiceMember(owner, fallbackGenerator)
                .queueAfter(1, TimeUnit.SECONDS)

            return false
        }

        ////////////////
        /// COOLDOWN ///
        ////////////////
        if (interactionCooldownService.acquire(data.userId, GENERATOR_COOLDOWN_KEY, GENERATOR_COOLDOWN) > Duration.ZERO) {
            return false
        }

        /////////////////////////////
        /// TEMPORARY VC SETTINGS ///
        /////////////////////////////
        val nameTemplate = VariablesManager.getNameTemplateForCreation(settings)

        if (!premiumManager.canUseVCNameTemplate(guildSettings, nameTemplate)) {
            throw PremiumConfigurationErrors.premiumVariables(
                guildId = guild.id,
                encounteredIn = "generating a temporary VC with the name $nameTemplate",
            )
        }

        val incrementalPosition =
            if (VariablesManager.doesTemplateRequireVCPositionalData(nameTemplate)) {
                vcPositionManager.getIncrementalPosition(
                    generatorId = settings.id,
                    excludedVCId = null,
                    temporaryVCs = data.temporaryVCEntities,
                )
            } else {
                null
            }
        val rawPosition = vcPositionManager.getRawPosition(incrementalPosition, settings, generatorVC)

        val bitrate =
            settings.bitrate
                ?.takeIf { it != 0 }
                ?.coerceIn(8000, guild.maxBitrate)
                ?: 64000
        val userLimit = (settings.userLimit ?: 0).coerceIn(0, VoiceChannel.MAX_USERLIMIT)
        val name = VariablesManager.computeVcNameForCreation(nameTemplate, locale, owner, bitrate, userLimit, incrementalPosition)

        val temporaryVCBuilder =
            generatorVC
                .createCopy()
                .setName(name)
                .apply { category?.also { setParent(it) } }
                .setUserlimit(userLimit)
                .setBitrate(bitrate)
                .setPosition(rawPosition)
                .reason(L.t(locale, "reason.vc.created"))

        // permissions
        val permissionOverrides: List<PermissionOverride> =
            when (settings.permissionsInherited) {
                GeneratorSettings.PermissionsInherited.NONE -> {
                    temporaryVCBuilder.clearPermissionOverrides()
                    emptyList()
                }
                GeneratorSettings.PermissionsInherited.GENERATOR -> generatorVC.permissionOverrides
                GeneratorSettings.PermissionsInherited.CATEGORY -> {
                    if (category != null) temporaryVCBuilder.syncPermissionOverrides()

                    category?.permissionOverrides ?: emptyList()
                }
            }

        // denied permissions based on initial state
        settings.initialState.permissionDenied?.also { deniedPermission ->
            val targetRole =
                guild.getRoleById(settings.permissionsTargetRole ?: guild.id)
                    ?: throw ConfigurationException(
                        ConfigurationErrorEntity(
                            guildId = guild.id,
                            code = ConfigurationErrorCode.GENERATOR_TARGET_ROLE_MISSING,
                            args = mapOf("generator" to generatorVC.name),
                        ),
                    )

            temporaryVCBuilder.modifyPermissionOverride(
                permissionOverride = permissionOverrides.firstOrNull { it.id == targetRole.id },
                permissionHolder = targetRole,
                allow = 0L,
                deny = deniedPermission.rawValue,
            )
        }

        // bot permissions
        temporaryVCBuilder.addMemberPermissionOverride(guild.selfMember.idLong, PermissionSets.astroVCPermissions, 0L)

        // immune role permissions (moderator role)
        settings.permissionsImmuneRole
            ?.let { guild.getRoleById(it) }
            ?.also { temporaryVCBuilder.addRolePermissionOverride(it.idLong, PermissionSets.immuneRoleVCPermissions, 0L) }

        // owner permissions
        temporaryVCBuilder.modifyPermissionOverride(
            permissionOverride = permissionOverrides.firstOrNull { it.id == owner.id },
            permissionHolder = owner,
            allow = settings.ownerPermissions?.takeIf { it != 0L } ?: PermissionSets.ownerVCPermissions,
            deny = 0L,
        )

        /////////////////////////////
        /// TEMPORARY VC CREATION ///
        /////////////////////////////
        val temporaryVC =
            try {
                temporaryVCBuilder.await()
            } catch (e: Exception) {
                throw unknownError(guild.id, "creating a temporary VC", e)
            }

        try {
            guild.moveVoiceMember(owner, temporaryVC).await()
        } catch (e: Exception) {
            temporaryVC.delete().reason(L.t(locale, "reason.vc.deleted_move_failed")).queueAfter(1, TimeUnit.SECONDS)
            throw unknownError(guild.id, "moving a user (${owner.id}) into a temporary VC", e)
        }

        ///////////////////////////////
        /// TEXT & WAITING CREATION ///
        ///////////////////////////////
        val privateChat =
            if (settings.autoChat) {
                if (!premiumManager.canCreatePrivateChatOnVCGeneration(guildSettings)) {
                    throw PremiumConfigurationErrors.autoPrivateChat(guild.id)
                }

                vcPrivateChatManager.create(owner, settings, temporaryVC, locale)
            } else {
                null
            }

        val waitingRoom =
            if (settings.autoWaiting) {
                if (!premiumManager.canCreateWaitingRoomOnVCGeneration(guildSettings)) {
                    throw PremiumConfigurationErrors.autoWaitingRoom(guild.id)
                }

                vcWaitingRoomManager.create(owner, settings, temporaryVC, incrementalPosition, locale)
            } else {
                null
            }

        ////////////////////
        /// CHAT MESSAGE ///
        ////////////////////
        computeCreationMessage(event, temporaryVC)?.also { message ->
            if (premiumManager.canSendMessageInVCChatOnVCGeneration(guildSettings)) {
                (privateChat ?: temporaryVC).sendMessage(message).queue()
            } else {
                configurationErrorEventPublisher.publishConfigurationErrorEvent(
                    PremiumConfigurationErrors.creationChatMessage(guild.id).configurationErrorEntity,
                )
            }
        }

        ///////////////////////////////
        /// STORE TEMPORARY VC DATA ///
        ///////////////////////////////
        // check if user is still in the created temporary vc before saving the data
        if (owner.voiceState?.channel?.id != temporaryVC.id) {
            val reason = L.t(locale, "reason.vc.deleted_left_too_quickly")

            waitingRoom?.delete()?.reason(reason)?.queueAfter(1, TimeUnit.SECONDS)
            privateChat?.delete()?.reason(reason)?.queueAfter(2, TimeUnit.SECONDS)
            temporaryVC.delete().reason(reason).queueAfter(3, TimeUnit.SECONDS)

            return false
        }

        temporaryVCRepository.save(
            guild.id,
            TemporaryVCEntity(
                id = temporaryVC.id,
                ownerID = owner.id,
                generatorID = settings.id,
                state = TemporaryVCEntity.TemporaryVCState.valueOf(settings.initialState.name),
                incrementalPosition = incrementalPosition,
                chatID = privateChat?.id,
                waitingID = waitingRoom?.id,
            ),
        )

        //////////////////
        /// OWNER ROLE ///
        //////////////////
        settings.ownerRole
            ?.let { guild.getRoleById(it) }
            ?.also {
                if (!premiumManager.canAssignTemporaryVCOwnerRole(guildSettings)) {
                    throw PremiumConfigurationErrors.ownerRole(guild.id)
                }

                memberRolesManager.add(it)
            }

        return true
    }

    private fun computeCreationMessage(
        event: VCEvent.JoinedGenerator,
        temporaryVC: VoiceChannel,
    ): MessageCreateData? {
        val settings = event.generatorSettings

        val interfaceData =
            settings.chatInterface
                ?.takeIf { it > -1 }
                ?.let { event.vcEventData.guildSettings.interfaces.getOrNull(it) }

        if (interfaceData != null) {
            return interfaceManager.computeMessage(interfaceData)
        }

        val template = settings.defaultChatText ?: return null
        val content = VariablesManager.computeChatMessage(template, event.vcEventData.guildSettings.supportedLocale, event.vcEventData.member, temporaryVC)

        return Containers.message(
            if (settings.defaultChatTextEmbed) {
                Containers.default(content)
            } else {
                Containers.text(content)
            },
        )
    }

    private fun unknownError(
        guildId: String,
        encounteredIn: String,
        cause: Exception,
    ) = ConfigurationException(
        ConfigurationErrorEntity(
            guildId = guildId,
            code = ConfigurationErrorCode.UNKNOWN,
            args = mapOf("encounteredIn" to encounteredIn, "details" to (cause.message ?: "")),
        ),
    )
}
