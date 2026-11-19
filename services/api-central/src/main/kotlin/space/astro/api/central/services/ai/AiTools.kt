package space.astro.api.central.services.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.stereotype.Component
import space.astro.api.central.controllers.guild_settings.validateGeneratorPremiumRequirements
import space.astro.api.central.models.requests.GeneratorCreateBody
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.APremiumRequiredException
import space.astro.shared.core.models.database.AiDiffEntry
import space.astro.shared.core.models.database.guildSettings.ConnectionData
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.models.database.guildSettings.TemplateData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import tools.jackson.databind.json.JsonMapper

/**
 * Executes the tools of [AiToolDescriptors].
 *
 * Read tools answer with JSON. Write tools never write: they validate the change, store a proposal
 * and answer with its summary, the change is written only when a user applies the proposal.
 * Errors are thrown as [space.astro.shared.core.exceptions.AException] and relayed to the model as plain sentences.
 */
@Component
class AiTools(
    private val guildSettingsRepository: GuildSettingsRepository,
    private val botApiClient: BotApiClient,
    private val premiumManager: PremiumManager,
    private val proposalService: AiProposalService,
    private val jsonMapper: JsonMapper,
) : AiToolExecutor {
    override fun execute(
        context: AiToolContext,
        name: String,
        args: JsonObject,
    ): AiToolOutput {
        val guildSettings =
            guildSettingsRepository.findByGuildID(context.guildId)
                ?: throw ANotFoundException("Astro has no settings for this server yet, open the dashboard once to create them")

        return when (name) {
            AiToolDescriptors.GET_GUILD_OVERVIEW.name -> json(guildOverview(guildSettings))
            AiToolDescriptors.LIST_CHANNELS_AND_CATEGORIES.name -> json(listChannels(context.guildId))
            AiToolDescriptors.LIST_ROLES.name ->
                json(
                    botApiClient
                        .getGuildRoles(
                            context.guildId,
                        ).map { mapOf("id" to it.id, "name" to it.name, "assignable" to it.manageable) },
                )
            AiToolDescriptors.LIST_GENERATORS.name -> json(listGenerators(guildSettings))
            AiToolDescriptors.GET_GENERATOR.name -> json(findGenerator(guildSettings, args.requireString("generator_id")))
            AiToolDescriptors.LIST_INTERFACES.name ->
                json(
                    guildSettings.interfaces.map {
                        mapOf("interface_id" to it.messageID, "channel_id" to it.channelID, "buttons" to it.buttons.size)
                    },
                )
            AiToolDescriptors.GET_INTERFACE.name -> json(findInterface(guildSettings, args.requireString("interface_id")))
            AiToolDescriptors.LIST_TEMPLATES.name -> json(guildSettings.templates)
            AiToolDescriptors.LIST_VOICE_ROLES.name -> json(guildSettings.connections)
            AiToolDescriptors.GET_PREMIUM_STATUS.name -> json(premiumStatus(guildSettings))
            AiToolDescriptors.CREATE_VOICE_GENERATOR.name -> createVoiceGenerator(context, guildSettings, args)
            AiToolDescriptors.UPDATE_GENERATOR.name -> updateGenerator(context, guildSettings, args)
            AiToolDescriptors.DELETE_GENERATOR.name -> deleteGenerator(context, guildSettings, args)
            AiToolDescriptors.CREATE_INTERFACE.name -> createInterface(context, guildSettings, args)
            AiToolDescriptors.SET_INTERFACE_BUTTONS.name -> setInterfaceButtons(context, guildSettings, args)
            AiToolDescriptors.CREATE_TEMPLATE.name -> createTemplate(context, guildSettings, args)
            AiToolDescriptors.UPDATE_TEMPLATE.name -> updateTemplate(context, guildSettings, args)
            AiToolDescriptors.CREATE_VOICE_ROLE.name -> createVoiceRole(context, guildSettings, args)
            AiToolDescriptors.DELETE_VOICE_ROLE.name -> deleteVoiceRole(context, guildSettings, args)
            else -> throw ABadRequestException("There is no tool named $name")
        }
    }

    private fun json(value: Any?) = AiToolOutput(jsonMapper.writeValueAsString(value))

    private fun guildOverview(guildSettings: GuildSettingsEntity) =
        mapOf(
            "locale" to guildSettings.locale,
            "premium" to premiumManager.isGuildPremium(guildSettings),
            "generators" to guildSettings.generators.size,
            "interfaces" to guildSettings.interfaces.size,
            "templates" to guildSettings.templates.size,
            "voice_roles" to guildSettings.connections.size,
            "updates_channel_id" to guildSettings.updatesChannelId,
        )

    private fun listChannels(guildId: String) =
        botApiClient.getGuildChannels(guildId).map {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "type" to
                    when (it.type) {
                        0 -> "text"
                        2 -> "voice"
                        4 -> "category"
                        5 -> "announcement"
                        13 -> "stage"
                        15 -> "forum"
                        else -> "other"
                    },
                "category_id" to it.parentId,
            )
        }

    private fun listGenerators(guildSettings: GuildSettingsEntity): List<Map<String, Any?>> {
        val channelNames =
            try {
                botApiClient.getGuildChannels(guildSettings.guildID).associate { it.id to it.name }
            } catch (_: Exception) {
                emptyMap()
            }

        return guildSettings.generators.map {
            mapOf(
                "generator_id" to it.id,
                "channel_name" to channelNames[it.id],
                "default_name" to it.defaultName,
                "user_limit" to it.userLimit,
                "category_id" to it.category,
            )
        }
    }

    private fun premiumStatus(guildSettings: GuildSettingsEntity) =
        mapOf(
            "premium" to premiumManager.isGuildPremium(guildSettings),
            "free_max_generators" to PremiumManager.FREE_MAX_GENERATORS,
            "free_max_interfaces" to PremiumManager.FREE_MAX_INTERFACES,
            "free_max_templates" to PremiumManager.FREE_MAX_TEMPLATES,
            "free_max_voice_roles" to PremiumManager.FREE_MAX_CONNECTIONS,
            "premium_only" to
                listOf(
                    "fallback generator",
                    "owner role",
                    "private chat",
                    "waiting room",
                    "chat message",
                    "interface message and button order",
                ),
        )

    private fun findGenerator(
        guildSettings: GuildSettingsEntity,
        generatorId: String,
    ) = guildSettings.generators.firstOrNull { it.id == generatorId }
        ?: throw ANotFoundException("There is no generator with id $generatorId, use list_generators to get the ids")

    private fun findInterface(
        guildSettings: GuildSettingsEntity,
        interfaceId: String,
    ) = guildSettings.interfaces.firstOrNull { it.messageID == interfaceId }
        ?: throw ANotFoundException("There is no interface with id $interfaceId, use list_interfaces to get the ids")

    private fun findTemplate(
        guildSettings: GuildSettingsEntity,
        templateId: String,
    ) = guildSettings.templates.firstOrNull { it.id == templateId }
        ?: throw ANotFoundException("There is no template with id $templateId, use list_templates to get the ids")

    private fun propose(
        context: AiToolContext,
        action: String,
        summary: String,
        targetId: String?,
        payload: Any?,
        diff: List<AiDiffEntry>,
        baseVersion: String?,
    ): AiToolOutput {
        val proposal = proposalService.create(context, action, summary, targetId, payload, diff, baseVersion)
        return AiToolOutput(
            text =
                "Proposal ${proposal.id} created: $summary. Changes: ${jsonMapper.writeValueAsString(diff)}. " +
                    "Nothing is saved yet, the user has to approve it with the button shown under your message.",
            proposal = proposal,
        )
    }

    private fun createVoiceGenerator(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        if (!premiumManager.canCreateGenerator(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_GENERATORS} generators")
        }

        val body = GeneratorCreateBody(name = args.requireString("name"), categoryId = args.string("category_id"))
        body.validate().throwIfInvalid()
        val userLimit = args.int("user_limit")
        if (userLimit != null && userLimit !in 0..99) {
            throw ABadRequestException("The user limit must be between 0 and 99")
        }

        val payload = AiGeneratorCreatePayload(body.name, body.categoryId, userLimit)
        return propose(
            context,
            AiToolDescriptors.CREATE_VOICE_GENERATOR.name,
            "Create the generator ${body.name}",
            null,
            payload,
            proposalService.diff(null, payload),
            null,
        )
    }

    private fun updateGenerator(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        val current = findGenerator(guildSettings, args.requireString("generator_id"))
        val updated =
            current.copy(
                defaultName = args.string("default_name") ?: current.defaultName,
                defaultLockedName = args.string("default_locked_name") ?: current.defaultLockedName,
                defaultHiddenName = args.string("default_hidden_name") ?: current.defaultHiddenName,
                userLimit = args.int("user_limit") ?: current.userLimit,
                bitrate = args.int("bitrate") ?: current.bitrate,
                category = args.string("category_id") ?: current.category,
                initialState = args.string("initial_state")?.let { GeneratorSettings.VCState.valueOf(it) } ?: current.initialState,
                queueMode = args.boolean("queue_mode") ?: current.queueMode,
                autoChat = args.boolean("auto_chat") ?: current.autoChat,
                autoWaiting = args.boolean("auto_waiting") ?: current.autoWaiting,
                ownerRole = args.string("owner_role_id") ?: current.ownerRole,
                fallbackId = args.string("fallback_generator_id") ?: current.fallbackId,
            )
        updated.validate().throwIfInvalid()
        premiumManager.validateGeneratorPremiumRequirements(guildSettings, current, updated)

        // validate() also fills the owner permissions from their ids, the current value is normalized the same way
        val diff = proposalService.diff(current.copy().also { it.validate() }, updated)
        if (diff.isEmpty()) {
            throw ABadRequestException("These values are already set, nothing to change")
        }

        return propose(
            context,
            AiToolDescriptors.UPDATE_GENERATOR.name,
            "Update the generator ${current.id}",
            current.id,
            updated,
            diff,
            proposalService.version(current),
        )
    }

    private fun deleteGenerator(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        val current = findGenerator(guildSettings, args.requireString("generator_id"))
        return propose(
            context,
            AiToolDescriptors.DELETE_GENERATOR.name,
            "Delete the generator ${current.id} and its voice channel",
            current.id,
            null,
            listOf(AiDiffEntry("generator", current.id, null)),
            proposalService.version(current),
        )
    }

    private fun createInterface(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        if (!premiumManager.canCreateInterface(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_INTERFACES} interface")
        }

        val channelId = args.requireString("channel_id")
        return propose(
            context,
            AiToolDescriptors.CREATE_INTERFACE.name,
            "Send a new interface in the channel $channelId",
            null,
            mapOf("channel_id" to channelId),
            listOf(AiDiffEntry("channel_id", null, channelId)),
            null,
        )
    }

    private fun setInterfaceButtons(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        val current = findInterface(guildSettings, args.requireString("interface_id"))
        val enabledIds = args.stringList("enabled_button_ids") ?: throw ABadRequestException("enabled_button_ids is required")
        val unknownIds = enabledIds - current.buttons.map { it.id }.toSet()
        if (unknownIds.isNotEmpty()) {
            throw ABadRequestException("The interface has no buttons with ids $unknownIds, use get_interface to get the ids")
        }

        val updated = current.copy(buttons = current.buttons.map { it.copy(buttonDisabled = it.id !in enabledIds) }.toMutableList())
        val diff =
            current.buttons.zip(updated.buttons).mapNotNull { (before, after) ->
                if (before.buttonDisabled == after.buttonDisabled) {
                    null
                } else {
                    AiDiffEntry(
                        "button ${before.name ?: before.id}",
                        if (before.buttonDisabled) "disabled" else "enabled",
                        if (after.buttonDisabled) "disabled" else "enabled",
                    )
                }
            }
        if (diff.isEmpty()) {
            throw ABadRequestException("These buttons are already enabled, nothing to change")
        }

        return propose(
            context,
            AiToolDescriptors.SET_INTERFACE_BUTTONS.name,
            "Change the enabled buttons of the interface ${current.messageID}",
            current.messageID,
            updated,
            diff,
            proposalService.version(current),
        )
    }

    private fun createTemplate(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        if (!premiumManager.canCreateTemplate(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_TEMPLATES} templates")
        }

        val template =
            TemplateData(
                name = args.requireString("name"),
                vcName = args.string("vc_name"),
                vcLimit = args.int("vc_limit"),
                vcBitrate = args.int("vc_bitrate"),
                enabledGeneratorIds = args.stringList("enabled_generator_ids")?.toMutableList(),
            )
        template.validate().throwIfInvalid()

        return propose(
            context,
            AiToolDescriptors.CREATE_TEMPLATE.name,
            "Create the template ${template.name}",
            null,
            template,
            proposalService.diff(null, template.copy(id = "")).filter { it.field != "id" },
            null,
        )
    }

    private fun updateTemplate(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        val current = findTemplate(guildSettings, args.requireString("template_id"))
        val updated =
            current.copy(
                name = args.string("name") ?: current.name,
                vcName = args.string("vc_name") ?: current.vcName,
                vcLimit = args.int("vc_limit") ?: current.vcLimit,
                vcBitrate = args.int("vc_bitrate") ?: current.vcBitrate,
            )
        updated.validate().throwIfInvalid()

        val diff = proposalService.diff(current, updated)
        if (diff.isEmpty()) {
            throw ABadRequestException("These values are already set, nothing to change")
        }

        return propose(
            context,
            AiToolDescriptors.UPDATE_TEMPLATE.name,
            "Update the template ${current.name}",
            current.id,
            updated,
            diff,
            proposalService.version(current),
        )
    }

    private fun createVoiceRole(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        if (!premiumManager.canCreateConnection(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_CONNECTIONS} voice role")
        }

        val connection =
            ConnectionData(
                id = args.requireString("channel_id"),
                roleID = args.requireString("role_id"),
                action =
                    args.string("action")?.let { ConnectionData.ConnectionAction.valueOf(it) } ?: ConnectionData.ConnectionAction.ASSIGN,
                permanent = args.boolean("permanent") ?: false,
            )
        connection.validate().throwIfInvalid()
        if (guildSettings.connections.any { it.id == connection.id && it.roleID == connection.roleID }) {
            throw ABadRequestException("A voice role for this channel and role already exists")
        }

        return propose(
            context,
            AiToolDescriptors.CREATE_VOICE_ROLE.name,
            "Create a voice role that does ${connection.action} of the role ${connection.roleID} in the channel ${connection.id}",
            null,
            connection,
            proposalService.diff(null, connection),
            null,
        )
    }

    private fun deleteVoiceRole(
        context: AiToolContext,
        guildSettings: GuildSettingsEntity,
        args: JsonObject,
    ): AiToolOutput {
        val channelId = args.requireString("channel_id")
        val roleId = args.requireString("role_id")
        val current =
            guildSettings.connections.firstOrNull { it.id == channelId && it.roleID == roleId }
                ?: throw ANotFoundException("There is no voice role for this channel and role, use list_voice_roles")

        return propose(
            context,
            AiToolDescriptors.DELETE_VOICE_ROLE.name,
            "Delete the voice role of the role $roleId in the channel $channelId",
            "$channelId:$roleId",
            null,
            proposalService.diff(current, null),
            proposalService.version(current),
        )
    }
}

/**
 * Payload of a generator creation, the voice channel is created on Discord only when the proposal is applied
 */
data class AiGeneratorCreatePayload(
    val name: String,
    val categoryId: String?,
    val userLimit: Int?,
)

private fun JsonObject.string(name: String) = this[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonObject.requireString(name: String) = string(name) ?: throw ABadRequestException("$name is required")

private fun JsonObject.int(name: String) = this[name]?.jsonPrimitive?.intOrNull

private fun JsonObject.boolean(name: String) = this[name]?.jsonPrimitive?.booleanOrNull

private fun JsonObject.stringList(name: String) = this[name]?.jsonArray?.map { it.jsonPrimitive.content }
