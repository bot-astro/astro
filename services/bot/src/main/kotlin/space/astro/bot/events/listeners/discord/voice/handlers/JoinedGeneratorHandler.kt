package space.astro.bot.events.listeners.discord.voice.handlers

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.stereotype.Component
import space.astro.bot.components.InterfaceManager
import space.astro.bot.components.managers.vc.VCPositionManager
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
import space.astro.bot.components.managers.vc.VariablesManager
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.interactions.InteractionCooldownService
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.bot.ui.Embeds
import space.astro.bot.utils.extensions.modifyPermissionOverride
import space.astro.shared.core.models.database.ConfigurationErrorEntity
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

        val generatorVC =
            data.joinedChannel?.asVoiceChannel()
                ?: throw IllegalStateException("Received joined generator event with a null joined channel")

        // TODO: premium, the legacy code checked the maximum amount of generators here

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
                            description =
                                "Your category for temporary VCs is full and you haven't configured a fallback generator." +
                                    "\nThis means Astro could not generate a temporary VC because the category was already full, " +
                                    "consider creating a fallback generator." +
                                    "\nEncountered in: ${generatorVC.name} generator",
                            guide = ConfigurationErrorEntity.Guide.GENERATOR,
                        ),
                    )

            // TODO: premium, the legacy code required premium to use a fallback generator
            guild.moveVoiceMember(owner, fallbackGenerator).queueAfter(1, TimeUnit.SECONDS)

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
        // TODO: premium, the legacy code required premium for some name variables
        val nameTemplate = VariablesManager.getNameTemplateForCreation(settings)

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
        val name = VariablesManager.computeVcNameForCreation(nameTemplate, owner, bitrate, userLimit, incrementalPosition)

        val temporaryVCBuilder =
            generatorVC
                .createCopy()
                .setName(name)
                .apply { category?.also { setParent(it) } }
                .setUserlimit(userLimit)
                .setBitrate(bitrate)
                .setPosition(rawPosition)

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
                            description =
                                "The generator ${generatorVC.name} has a target role set " +
                                    "but Astro couldn't find that role in your server!",
                            guide = ConfigurationErrorEntity.Guide.GENERATOR,
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
                throw unknownError(guild.id, "creating a temporary VC: ${e.message}")
            }

        try {
            guild.moveVoiceMember(owner, temporaryVC).await()
        } catch (e: Exception) {
            temporaryVC.delete().reason("Unknown error moving a user into a temporary VC").queueAfter(1, TimeUnit.SECONDS)
            throw unknownError(guild.id, "moving a user (${owner.id}) into a temporary VC: ${e.message}")
        }

        ///////////////////////////////
        /// TEXT & WAITING CREATION ///
        ///////////////////////////////
        // TODO: premium, the legacy code required premium for the automatic private chat and waiting room
        val privateChat =
            if (settings.autoChat) {
                vcPrivateChatManager.create(owner, settings, temporaryVC)
            } else {
                null
            }

        val waitingRoom =
            if (settings.autoWaiting) {
                vcWaitingRoomManager.create(owner, settings, temporaryVC, incrementalPosition)
            } else {
                null
            }

        ////////////////////
        /// CHAT MESSAGE ///
        ////////////////////
        // TODO: premium, the legacy code required premium to send the creation message
        computeCreationMessage(event, temporaryVC)?.also { message ->
            (privateChat ?: temporaryVC).sendMessage(message).queue()
        }

        ///////////////////////////////
        /// STORE TEMPORARY VC DATA ///
        ///////////////////////////////
        // check if user is still in the created temporary vc before saving the data
        if (owner.voiceState?.channel?.id != temporaryVC.id) {
            val reason = "User left the generated temporary VC too quickly"

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
        // TODO: premium, the legacy code required premium to assign the owner role
        settings.ownerRole
            ?.let { guild.getRoleById(it) }
            ?.also { memberRolesManager.add(it) }

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
        val content = VariablesManager.computeChatMessage(template, event.vcEventData.member, temporaryVC)

        // TODO: components v2, replace the embed (last step of the migration)
        return MessageCreateBuilder()
            .useComponentsV2(false)
            .apply {
                if (settings.defaultChatTextEmbed) {
                    setEmbeds(Embeds.default(content))
                } else {
                    setContent(content)
                }
            }.build()
    }

    private fun unknownError(
        guildId: String,
        encounteredIn: String,
    ) = ConfigurationException(
        ConfigurationErrorEntity(
            guildId = guildId,
            description = "An unknown error occurred!\nEncountered in: $encounteredIn",
        ),
    )
}
