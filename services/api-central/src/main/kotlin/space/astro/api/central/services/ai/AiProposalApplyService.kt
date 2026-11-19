package space.astro.api.central.services.ai

import org.springframework.stereotype.Service
import space.astro.api.central.controllers.guild_settings.validateGeneratorPremiumRequirements
import space.astro.api.central.models.ai.AiProposal
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.exceptions.AConflictException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.APremiumRequiredException
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.api.bot.request.ChannelCreateBotApiRequest
import space.astro.shared.core.models.api.bot.request.InterfaceCreateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.ConnectionData
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.models.database.guildSettings.TemplateData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

/**
 * Applies or cancels the proposals of the assistant, the permissions of the user are checked by the callers
 */
@Service
class AiProposalApplyService(
    private val proposalService: AiProposalService,
    private val auditService: AiAuditService,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val botApiClient: BotApiClient,
    private val premiumManager: PremiumManager,
    private val jsonMapper: JsonMapper,
) {
    /**
     * Writes the exact payload of the proposal
     *
     * @return the guild settings after the change
     * @throws AConflictException if the proposal expired or its target changed since it was created
     */
    fun apply(
        guildId: String,
        userId: String,
        proposalId: String,
    ): GuildSettingsEntity {
        val proposal = getOwnedProposal(guildId, userId, proposalId)
        if (proposal.expiresAt < System.currentTimeMillis()) {
            proposalService.delete(proposal.id)
            throw AConflictException("This proposal expired, ask the assistant again")
        }

        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (proposal.baseVersion != null && proposalService.version(findTarget(guildSettings, proposal)) != proposal.baseVersion) {
            proposalService.delete(proposal.id)
            throw AConflictException("The settings changed since this proposal was made, ask the assistant again")
        }

        // deleted before writing so that a double click cannot apply it twice
        proposalService.delete(proposal.id)
        applyPayload(guildSettings, proposal)
        guildSettingsRepository.save(guildSettings)
        auditService.recordApplied(proposal, userId)

        return guildSettings
    }

    fun cancel(
        guildId: String,
        userId: String,
        proposalId: String,
    ) {
        getOwnedProposal(guildId, userId, proposalId)
        proposalService.delete(proposalId)
    }

    private fun getOwnedProposal(
        guildId: String,
        userId: String,
        proposalId: String,
    ): AiProposal {
        val proposal =
            proposalService.get(proposalId)
                ?: throw ANotFoundException("Proposal with ID $proposalId not found")

        if (proposal.guildId != guildId || proposal.userId != userId) {
            throw AUnauthorizedException("Only the user that asked for the proposal can apply or cancel it")
        }

        return proposal
    }

    private fun findTarget(
        guildSettings: GuildSettingsEntity,
        proposal: AiProposal,
    ): Any? =
        when (proposal.action) {
            AiToolDescriptors.UPDATE_GENERATOR.name, AiToolDescriptors.DELETE_GENERATOR.name ->
                guildSettings.generators.firstOrNull { it.id == proposal.targetId }
            AiToolDescriptors.SET_INTERFACE_BUTTONS.name -> guildSettings.interfaces.firstOrNull { it.messageID == proposal.targetId }
            AiToolDescriptors.UPDATE_TEMPLATE.name -> guildSettings.templates.firstOrNull { it.id == proposal.targetId }
            AiToolDescriptors.DELETE_VOICE_ROLE.name ->
                guildSettings.connections.firstOrNull { "${it.id}:${it.roleID}" == proposal.targetId }
            else -> null
        }

    private fun applyPayload(
        guildSettings: GuildSettingsEntity,
        proposal: AiProposal,
    ) {
        val guildId = guildSettings.guildID
        val payload = proposal.payload.orEmpty()

        when (proposal.action) {
            AiToolDescriptors.CREATE_VOICE_GENERATOR.name -> {
                if (!premiumManager.canCreateGenerator(guildSettings)) {
                    throw APremiumRequiredException(
                        "Upgrade the server to create more than ${PremiumManager.FREE_MAX_GENERATORS} generators",
                    )
                }
                val create = jsonMapper.readValue<AiGeneratorCreatePayload>(payload)
                val channel =
                    botApiClient.createChannel(
                        guildId,
                        ChannelCreateBotApiRequest(
                            name = create.name,
                            categoryId = create.categoryId,
                            channelType = ChannelCreateBotApiRequest.ChannelCreateType.VOICE,
                        ),
                    )
                guildSettings.generators.add(
                    GeneratorSettings(
                        id = channel.id,
                        category = create.categoryId,
                        chatCategory = create.categoryId,
                        waitingCategory = create.categoryId,
                        userLimit = create.userLimit,
                        // the variables are kept as they are, they get replaced when the temporary VC is created
                        defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default", "nickname" to "{nickname}"),
                        defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default", "vc_name" to "{vc_name}"),
                        defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default", "vc_name" to "{vc_name}"),
                    ),
                )
            }

            AiToolDescriptors.UPDATE_GENERATOR.name -> {
                val updated = jsonMapper.readValue<GeneratorSettings>(payload)
                val index = guildSettings.generators.indexOfFirst { it.id == proposal.targetId }
                premiumManager.validateGeneratorPremiumRequirements(guildSettings, guildSettings.generators[index], updated)
                guildSettings.generators[index] = updated
            }

            AiToolDescriptors.DELETE_GENERATOR.name -> {
                guildSettings.generators.removeIf { it.id == proposal.targetId }
                try {
                    botApiClient.deleteChannel(guildId, proposal.targetId!!)
                } catch (_: Exception) {
                    // the user might have already deleted the channel
                }
            }

            AiToolDescriptors.CREATE_INTERFACE.name -> {
                if (!premiumManager.canCreateInterface(guildSettings)) {
                    throw APremiumRequiredException(
                        "Upgrade the server to create more than ${PremiumManager.FREE_MAX_INTERFACES} interface",
                    )
                }
                val channelId = jsonMapper.readTree(payload).get("channel_id").asString()
                guildSettings.interfaces.add(botApiClient.createInterface(guildId, InterfaceCreateBotApiRequest(channelId = channelId)))
            }

            AiToolDescriptors.SET_INTERFACE_BUTTONS.name -> {
                val updated = jsonMapper.readValue<InterfaceData>(payload)
                botApiClient.updateInterface(guildId, updated)
                guildSettings.interfaces[guildSettings.interfaces.indexOfFirst { it.messageID == proposal.targetId }] = updated
            }

            AiToolDescriptors.CREATE_TEMPLATE.name -> {
                if (!premiumManager.canCreateTemplate(guildSettings)) {
                    throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_TEMPLATES} templates")
                }
                guildSettings.templates.add(jsonMapper.readValue<TemplateData>(payload))
            }

            AiToolDescriptors.UPDATE_TEMPLATE.name -> {
                val updated = jsonMapper.readValue<TemplateData>(payload)
                guildSettings.templates[guildSettings.templates.indexOfFirst { it.id == proposal.targetId }] = updated
            }

            AiToolDescriptors.CREATE_VOICE_ROLE.name -> {
                if (!premiumManager.canCreateConnection(guildSettings)) {
                    throw APremiumRequiredException(
                        "Upgrade the server to create more than ${PremiumManager.FREE_MAX_CONNECTIONS} voice role",
                    )
                }
                guildSettings.connections.add(jsonMapper.readValue<ConnectionData>(payload))
            }

            AiToolDescriptors.DELETE_VOICE_ROLE.name ->
                guildSettings.connections.removeIf { "${it.id}:${it.roleID}" == proposal.targetId }

            else -> throw AConflictException("Unknown proposal action ${proposal.action}")
        }
    }
}
