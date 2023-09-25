package space.astro.bot.listeners.voice.handlers

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import space.astro.bot.extentions.modifyPermissionOverride
import space.astro.bot.managers.interfaces.InterfaceManager
import space.astro.bot.managers.roles.SimpleMemberRolesManager
import space.astro.bot.managers.util.GuildErrorNotifier
import space.astro.bot.managers.util.PermissionSets
import space.astro.bot.managers.vc.*
import space.astro.bot.ui.Embeds
import space.astro.shared.core.models.database.PermissionsInherited
import space.astro.shared.core.models.database.TemporaryVCData
import java.util.concurrent.TimeUnit

/**
 * Handles the creation of temporary voice channels via generators
 *
 * @return true if the temporary voice channel has been created successfully, false otherwise
 */
suspend fun VCEventHandler.handleJoinedGeneratorEvent(
    event: VCEvent.JoinedGenerator,
    guildErrorNotifier: GuildErrorNotifier,
    memberRolesManager: SimpleMemberRolesManager,
): Boolean {
    val data = event.vcEventData
    val guild = data.guild
    val owner = data.member
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
        TODO()

        return false
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
        }

        if (availableTemporaryVC != null) {
            guild.moveVoiceMember(owner, availableTemporaryVC).queue(null) {
                TODO()
            }

            return false
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
            guild.moveVoiceMember(owner, fallbackGenerator)
                .queueAfter(1, TimeUnit.SECONDS, null) {
                    TODO()
                }
        } else {
            TODO("Send error")
        }

        return false
    }


    ////////////////
    /// COOLDOWN ///
    ////////////////

    val generatorCooldown = cooldownsManager.getUserGeneratorsCooldown(data.userId)
        .takeIf { it > 0 }

    if (generatorCooldown != null) {
        TODO("Send private message to user")
        return false
    }

    /////////////////////////////
    /// TEMPORARY VC SETTINGS ///
    /////////////////////////////

    val generatorVC = data.joinedChannel.asVoiceChannel()
    val nameTemplate = VariablesManager.getCreationNameTemplate(generatorData)

    // positional data
    val requiresPositionalData = VariablesManager.doesTemplateRequireVCPositionalData(nameTemplate)
    val incrementalPosition = if (requiresPositionalData) {
        VCPositionManager.getIncrementalPosition(
            generatorId = generatorData.id,
            excludedVCId = null,
            temporaryVCs = data.temporaryVCs
        )
    } else { null }
    val rawPosition = VCPositionManager.getRawPosition(incrementalPosition, generatorData, generatorVC)

    // bitrate, userlimit and name
    val bitrate = generatorData.bitrate
        .takeIf { it != 0 }
        ?.coerceAtMost(guild.maxBitrate)
        ?: 64000
    val userLimit = generatorData.userLimit
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

        if (targetRole == null) {
            TODO("")
            return false
        }

        val targetRolePermissionOverride = permissionOverrides.firstOrNull { it.id == targetRole.id }

        temporaryVCBuilder.modifyPermissionOverride(
            permissionOverride = targetRolePermissionOverride,
            target = targetRole,
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
            val immuneRolePermissionOverride = permissionOverrides.firstOrNull { it.id == role.id }
            temporaryVCBuilder.modifyPermissionOverride(
                permissionOverride = immuneRolePermissionOverride,
                target = role,
                allow = PermissionSets.immuneRoleVCPermissions,
                deny = 0L
            )
        }

    // owner permissions
    val ownerPermissions = generatorData.ownerPermissions.takeIf { it != 0L }
        ?: PermissionSets.ownerVCPermissions

    temporaryVCBuilder.modifyPermissionOverride(
        permissionOverride = permissionOverrides.firstOrNull { it.id == owner.id },
        target = owner,
        allow = ownerPermissions,
        deny = 0L
    )

    /////////////////////////////
    /// TEMPORARY VC CREATION ///
    /////////////////////////////

    val temporaryVC = try {
        temporaryVCBuilder.await()
    } catch (e: Exception) {
        TODO()
        return false
    }

    try {
        guild.moveVoiceMember(owner, temporaryVC).await()
    } catch (e: Exception) {
        temporaryVC.delete().queueAfter(1, TimeUnit.SECONDS, null) {
            TODO()
        }
        TODO()
        return false
    }

    ///////////////////////////////
    /// TEXT & WAITING CREATION ///
    ///////////////////////////////

    val textChat = if (generatorData.autoChat) {
        try {
            VCTextChatManager.create(
                owner = owner,
                generatorData = generatorData,
                temporaryVC = temporaryVC
            )
        } catch (e: Exception) {
            TODO()
            null
        }
    } else null

    val waitingVC = if (generatorData.autoWaiting) {
        try {
            VCWaitingRoomManager.create(
                owner = owner,
                generatorData = generatorData,
                temporaryVC = temporaryVC,
                temporaryVCIncrementalPosition = incrementalPosition
            )
        } catch (e: Exception) {
            TODO()
            null
        }
    } else null

    ////////////////////
    /// CHAT MESSAGE ///
    ////////////////////
    val chatForMessage = textChat ?: temporaryVC

    val interfaceToSend = generatorData.chatInterface
        .takeIf { it > -1 }
        ?.let {
            data.guildData.interfaces.getOrNull(it) // this is not a great approach
        }


    val interfaceMessage = if (interfaceToSend != null) {
        InterfaceManager.computeMessage(interfaceToSend)
    } else if (generatorData.defaultChatText != null) {
        val content = VariablesManager.computeChatMessage(generatorData.defaultChatText!!, owner, temporaryVC)

        MessageCreateBuilder()
            .apply {
                if (generatorData.defaultChatTextEmbed) {
                    setEmbeds(Embeds.withDescription(content))
                } else {
                    setContent(content)
                }
            }
            .build()
    } else
        null

    if (interfaceMessage != null) {
        chatForMessage.sendMessage(interfaceMessage).queue()
    }

    ///////////////////////////////
    /// STORE TEMPORARY VC DATA ///
    ///////////////////////////////

    // check if user is still in the created temporary vc before saving the data
    if (owner.voiceState!!.channel?.id != temporaryVC.id) {
        waitingVC?.delete()?.queueAfter(1000, TimeUnit.MILLISECONDS)
        textChat?.delete()?.queueAfter(2000, TimeUnit.SECONDS)
        temporaryVC.delete().queueAfter(3000, TimeUnit.SECONDS)

        cooldownsManager.markUserGeneratorsCooldown(data.userId)

        return false
    }

    val temporaryVCData = TemporaryVCData(
        id = temporaryVC.id,
        ownerId = owner.id,
        generatorId = generatorData.id,
        state = generatorData.initialState,
        incrementalPosition = incrementalPosition,
        chatID = textChat?.id
    )

    temporaryVCDao.save(guild.id, temporaryVCData)


    //////////////////
    /// OWNER ROLE ///
    //////////////////

    generatorData.ownerRole
        ?.takeIf { premiumRequirementDetector.isGuildPremium(data.guildData) }
        ?.let {
            guild.getRoleById(it)
        }?.also {
            memberRolesManager.add(it)
        }

    // TODO: generator statistics

    // TODO: first time user created a temporary vc private message

    return true
}

