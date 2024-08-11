package space.astro.bot.events.listeners.voice.handlers

import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import space.astro.shared.core.components.managers.VariablesManager
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.core.ui.Embeds
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.shared.core.models.database.PermissionsInherited
import space.astro.shared.core.models.database.TemporaryVCData
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {  }

/**
 * Handles the creation of temporary voice channels via generators
 *
 * @return true if the temporary voice channel has been created successfully, false otherwise
 */
suspend fun VCEventHandler.handleJoinedGeneratorEvent(
    event: VCEvent.JoinedGenerator,
    memberRolesManager: SimpleMemberRolesManager,
) {
    val data = event.vcEventData
    val guild = data.guild
    val guildId = guild.id
    val owner = data.member
    val guildData = data.guildData
    val generatorData = event.generatorData

    ///////////////////
    /// STATE CHECK ///
    ///////////////////

    if (data.joinedChannel == null) {
        throw IllegalStateException("Received joined generator event with a null joined channel")
    }

    //////////////////////////
    /// PREMIUM REQUISITES ///
    //////////////////////////
    if (premiumRequirementDetector.exceededMaximumGeneratorAmount(data.guildData)) {
        throw ConfigurationException(configurationErrorService.maximumAmountOfGenerator(guildId))
    }

    //////////////
    /// QUEUES ///
    //////////////
    if (generatorData.queueMode) {
        val generatorTemporaryVCsIds = data.temporaryVCs
            .filter { it.generatorId == generatorData.id }
            .map {
                it.id
            }

        val availableTemporaryVC = guild.voiceChannelCache.firstOrNull {
            it.id in generatorTemporaryVCsIds
                    && (it.userLimit == 0 || it.members.size < it.userLimit)
                    && owner.hasPermission(it, Permission.VOICE_CONNECT)
        }

        if (availableTemporaryVC != null) {
            guild.moveVoiceMember(owner, availableTemporaryVC).queue()
            return
        }
    }


    /////////////////////////////////////////
    /// CATEGORY CALCULATION AND FALLBACK ///
    /////////////////////////////////////////

    var category: Category? = null

    generatorData.category?.also {
        category = guild.getCategoryById(it)
    }

    if (category != null && category!!.channels.size >= 50) {
        val fallbackGenerator = generatorData.fallbackId
            ?.let { fallback ->
                data.guildData.generators.firstOrNull { it.id == fallback && it.id != data.leftChannel?.id }
            }
            ?.let {
                guild.getVoiceChannelById(it.id)
            }
            ?.takeIf {
                it.parentCategoryId != category!!.id
            }

        if (fallbackGenerator != null) {
            if (premiumRequirementDetector.canUseFallbackGenerator(guildData)) {
                guild.moveVoiceMember(owner, fallbackGenerator)
                    .queueAfter(1, TimeUnit.SECONDS)
            } else {
                throw ConfigurationException(configurationErrorService.premiumFallbackGenerator(guildId))
            }
        } else {
            throw ConfigurationException(configurationErrorService.missingFallbackGenerator(
                guildId = guildId,
                encounteredIn = "${data.joinedChannel.name} generator"
            ))
        }

        return
    }


    ////////////////
    /// COOLDOWN ///
    ////////////////

    val generatorCooldown = cooldownsManager.getUserGeneratorsCooldown(data.userId)
        .takeIf { it > 0 }

    if (generatorCooldown != null) {
        return
    }

    /////////////////////////////
    /// TEMPORARY VC SETTINGS ///
    /////////////////////////////

    val generatorVC = data.joinedChannel.asVoiceChannel()
    val nameTemplate = VariablesManager.getNameTemplateForCreation(generatorData)

    if (!premiumRequirementDetector.canUseVCNameTemplate(guildData, nameTemplate)) {
        throw ConfigurationException(configurationErrorService.premiumVariables(
            guildId = guildId,
            encounteredIn = "generating a temporary VC with the name $nameTemplate"
        ))
    }

    // positional data
    val requiresPositionalData = VariablesManager.doesTemplateRequireVCPositionalData(nameTemplate)
    val incrementalPosition = if (requiresPositionalData) {
        vcPositionManager.getIncrementalPosition(
            generatorId = generatorData.id,
            excludedVCId = null,
            temporaryVCs = data.temporaryVCs
        )
    } else { null }
    val rawPosition = vcPositionManager.getRawPosition(incrementalPosition, generatorData, generatorVC)

    // bitrate, userlimit and name
    val bitrate = generatorData.bitrate
        .takeIf { it != 0 }
        ?.coerceIn(8000, guild.maxBitrate)
        ?: 64000
    val userLimit = generatorData.userLimit.coerceIn(
        minimumValue = 0,
        maximumValue = VoiceChannel.MAX_USERLIMIT
    )
    val name = VariablesManager.computeVcNameForCreation(nameTemplate, owner, bitrate, userLimit, incrementalPosition)

    // channel builder initialization
    val temporaryVCBuilder = generatorVC.createCopy()
        .setName(name)
        .apply {
            if (category != null) {
                setParent(category)
            }
        }
        .setUserlimit(userLimit)
        .setBitrate(bitrate)
        .setPosition(rawPosition)

    // permissions
    // base overrides
    val permissionOverrides: List<PermissionOverride> = when (generatorData.permissionsInherited) {
        PermissionsInherited.NONE -> {
            temporaryVCBuilder.clearPermissionOverrides()
            emptyList()
        }
        PermissionsInherited.GENERATOR -> {
            generatorVC.permissionOverrides
        }
        PermissionsInherited.CATEGORY -> {
            if (category != null)
                temporaryVCBuilder.syncPermissionOverrides()

            category?.permissionOverrides ?: emptyList()
        }
    }

    // denied permissions based on initial state
    generatorData.initialState.permissionDenied?.also { deniedPermission ->
        val targetRole = guild.getRoleById(generatorData.permissionsTargetRole ?: guild.id)
            ?: throw ConfigurationException(configurationErrorService.missingGeneratorTargetRole(
                guildId = guildId,
                generatorName = data.joinedChannel.name
            ))

        val targetRolePermissionOverride = permissionOverrides.firstOrNull { it.id == targetRole.id }

        temporaryVCBuilder.modifyPermissionOverride(
            permissionOverride = targetRolePermissionOverride,
            permissionHolder = targetRole,
            allow = 0,
            deny = deniedPermission.rawValue
        )
    }

    // bot permissions
    temporaryVCBuilder.addMemberPermissionOverride(
        guild.selfMember.idLong,
        PermissionSets.astroVCPermissions,
        0L
    )

    // immune role permissions (moderator role)
    generatorData.permissionsImmuneRole
        ?.let { guild.getRoleById(it) }
        ?.also { role ->
//            val immuneRolePermissionOverride = permissionOverrides.firstOrNull { it.id == role.id }
//            temporaryVCBuilder.modifyPermissionOverride(
//                permissionOverride = immuneRolePermissionOverride,
//                permissionHolder = role,
//                allow = PermissionSets.immuneRoleVCPermissions,
//                deny = 0L
//            )
            temporaryVCBuilder.addRolePermissionOverride(
                role.idLong,
                PermissionSets.immuneRoleVCPermissions,
                0L
            )
        }

    // owner permissions
    val ownerPermissions = generatorData.ownerPermissions.takeIf { it != 0L }
        ?: PermissionSets.ownerVCPermissions

    temporaryVCBuilder.modifyPermissionOverride(
        permissionOverride = permissionOverrides.firstOrNull { it.id == owner.id },
        permissionHolder = owner,
        allow = ownerPermissions,
        deny = 0L
    )

    /////////////////////////////
    /// TEMPORARY VC CREATION ///
    /////////////////////////////

    val temporaryVC = try {
        temporaryVCBuilder.await()
    } catch (e: Exception) {
        throw ConfigurationException(
            configurationErrorService.unknown(
                guildId = guildId,
                encounteredIn = "creating a temporary VC: ${e.message}"
            )
        )
    }

    try {
        guild.moveVoiceMember(owner, temporaryVC).await()
    } catch (e: Exception) {
        log.info { "DELETE - Unknown error moving a user into a temporary VC  - server ${guild.id}" }
        temporaryVC.delete().reason("Unknown error moving a user into a temporary VC").queueAfter(1, TimeUnit.SECONDS)
        throw ConfigurationException(
            configurationErrorService.unknown(
                guildId = guildId,
                encounteredIn = "moving a user (${owner.id}) into a temporary VC: ${e.message}"
            )
        )
    }

    ///////////////////////////////
    /// TEXT & WAITING CREATION ///
    ///////////////////////////////

    val privateChat = if (generatorData.autoChat) {
        if (!premiumRequirementDetector.canCreatePrivateChatOnVCGeneration(guildData)) {
            throw ConfigurationException(configurationErrorService.premiumRequiredForAutoPrivateChatCreation(guildId))
        } else {
            vcPrivateChatManager.create(
                owner = owner,
                generatorData = generatorData,
                temporaryVC = temporaryVC
            )
        }
    } else null

    val waitingRoom = if (generatorData.autoWaiting) {
        if (!premiumRequirementDetector.canCreateWaitingRoomOnVCGeneration(guildData)) {
            throw ConfigurationException(configurationErrorService.premiumRequiredForAutoWaitingRoomCreation(guildId))
        } else {
            vcWaitingRoomManager.create(
                owner = owner,
                generatorData = generatorData,
                temporaryVC = temporaryVC,
                temporaryVCIncrementalPosition = incrementalPosition
            )
        }
    } else null

    ////////////////////
    /// CHAT MESSAGE ///
    ////////////////////
    val chatForMessage = privateChat ?: temporaryVC

    val interfaceToSend = generatorData.chatInterface
        .takeIf { it > -1 }
        ?.let {
            data.guildData.interfaces.getOrNull(it) // this is not a great approach
        }


    val creationChatMessage = if (interfaceToSend != null) {
        interfaceManager.computeMessage(interfaceToSend)
    } else if (generatorData.defaultChatText != null) {
        val content = VariablesManager.computeChatMessage(generatorData.defaultChatText!!, owner, temporaryVC)

        MessageCreateBuilder()
            .apply {
                if (generatorData.defaultChatTextEmbed) {
                    setEmbeds(Embeds.default(content))
                } else {
                    setContent(content)
                }
            }
            .build()
    } else
        null

    if (creationChatMessage != null) {
        if (!premiumRequirementDetector.canSendMessageInVCChatOnVCGeneration(guildData)) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                configurationErrorService.premiumRequiredForAutoChatMessageOnCreation(guildId)
            )
        } else {
            chatForMessage.sendMessage(creationChatMessage).queue()
        }
    }

    ///////////////////////////////
    /// STORE TEMPORARY VC DATA ///
    ///////////////////////////////

    // check if user is still in the created temporary vc before saving the data
    if (owner.voiceState!!.channel?.id != temporaryVC.id) {
        waitingRoom?.delete()
            ?.reason("User left the generated temporary VC too quickly")
            ?.queueAfter(1, TimeUnit.SECONDS)
        privateChat?.delete()
            ?.reason("User left the generated temporary VC too quickly")
            ?.queueAfter(2, TimeUnit.SECONDS)
        log.info { "DELETE - User left the generated temporary VC too quickly - server ${guild.id}" }
        temporaryVC.delete()
            .reason("User left the generated temporary VC too quickly").queueAfter(3, TimeUnit.SECONDS)

        cooldownsManager.markUserGeneratorsCooldown(data.userId)

        return
    }

    val temporaryVCData = TemporaryVCData(
        id = temporaryVC.id,
        ownerId = owner.id,
        generatorId = generatorData.id,
        state = generatorData.initialState,
        incrementalPosition = incrementalPosition,
        chatID = privateChat?.id,
        waitingID = waitingRoom?.id
    )

    temporaryVCDao.save(guild.id, temporaryVCData)


    //////////////////
    /// OWNER ROLE ///
    //////////////////

    generatorData.ownerRole
        ?.let { guild.getRoleById(it) }
        ?.also {
            if (!premiumRequirementDetector.canAssignTemporaryVCOwnerRole(guildData)) {
                throw ConfigurationException(configurationErrorService.premiumRequiredForOwnerRole(guildId))
            } else {
                memberRolesManager.add(it)
            }
        }
}