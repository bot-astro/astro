package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.GeneratorCreateBody
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.APremiumRequiredException
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.api.bot.request.ChannelCreateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Generators settings")
class GuildGeneratorsController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumManager: PremiumManager,
) {
    @Operation(summary = "Create generator")
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATORS)
    @CanManageGuildMiddleware
    fun createGenerator(
        @PathVariable guildId: String,
        @RequestBody generatorCreateBody: GeneratorCreateBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (!premiumManager.canCreateGenerator(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to create more than ${PremiumManager.FREE_MAX_GENERATORS} generators")
        }

        val generatorDiscordChannel =
            botApiClient.createChannel(
                guildId,
                ChannelCreateBotApiRequest(
                    name = generatorCreateBody.name,
                    categoryId = generatorCreateBody.categoryId,
                    channelType = ChannelCreateBotApiRequest.ChannelCreateType.VOICE,
                ),
            )

        val generatorSettings =
            GeneratorSettings(
                id = generatorDiscordChannel.id,
                // the variables are kept as they are, they get replaced when the temporary VC is created
                defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default", "nickname" to "{nickname}"),
                defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default", "vc_name" to "{vc_name}"),
                defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default", "vc_name" to "{vc_name}"),
            )

        guildSettings.generators.add(generatorSettings)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(summary = "Update generator")
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATOR)
    @CanManageGuildMiddleware
    fun updateGenerator(
        @PathVariable guildId: String,
        @PathVariable generatorId: String,
        @RequestBody updatedGeneratorSettings: GeneratorSettings,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val generatorIndex =
            guildSettings.generators
                .indexOfFirst { it.id == generatorId }
                .takeIf { it != -1 }
                ?: throw ANotFoundException(
                    "Generator with ID ${updatedGeneratorSettings.id} not found for guild with id $guildId",
                )

        validatePremiumRequirements(guildSettings, guildSettings.generators[generatorIndex], updatedGeneratorSettings)

        guildSettings.generators[generatorIndex] = updatedGeneratorSettings
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(summary = "Delete generator")
    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATOR)
    @CanManageGuildMiddleware
    fun deleteGenerator(
        @PathVariable guildId: String,
        @PathVariable generatorId: String,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        guildSettings.generators.removeIf { it.id == generatorId }
        try {
            botApiClient.deleteChannel(guildId, generatorId)
        } catch (_: Exception) {
            // we don't care if it fails deleting on Discord to be fair
            // as the user might have already deleted it
        }

        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    /**
     * Rejects premium settings that are being newly set, settings that were already there are left untouched
     * so that a guild without premium can still edit the rest of the generator
     *
     * @throws APremiumRequiredException
     */
    private fun validatePremiumRequirements(
        guildSettings: GuildSettingsEntity,
        current: GeneratorSettings,
        updated: GeneratorSettings,
    ) {
        if (updated.fallbackId != null &&
            updated.fallbackId != current.fallbackId &&
            !premiumManager.canUseFallbackGenerator(guildSettings)
        ) {
            throw APremiumRequiredException("Upgrade the server to use a fallback generator")
        }

        listOf(
            updated.defaultName to current.defaultName,
            updated.defaultLockedName to current.defaultLockedName,
            updated.defaultHiddenName to current.defaultHiddenName,
        ).forEach { (updatedName, currentName) ->
            if (updatedName != null &&
                updatedName != currentName &&
                !premiumManager.canUseVCNameTemplate(guildSettings, updatedName)
            ) {
                throw APremiumRequiredException("Some variables used in the name can only be used in upgraded servers")
            }
        }

        if (updated.ownerRole != null &&
            updated.ownerRole != current.ownerRole &&
            !premiumManager.canAssignTemporaryVCOwnerRole(guildSettings)
        ) {
            throw APremiumRequiredException("Upgrade the server to assign a role to the owner of temporary VCs")
        }

        if (updated.autoChat && !current.autoChat && !premiumManager.canCreatePrivateChatOnVCGeneration(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to automatically create a private chat")
        }

        if (updated.autoWaiting && !current.autoWaiting && !premiumManager.canCreateWaitingRoomOnVCGeneration(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to automatically create a waiting room")
        }

        val newChatMessage =
            (updated.defaultChatText != null && updated.defaultChatText != current.defaultChatText) ||
                (updated.chatInterface != null && updated.chatInterface != current.chatInterface)

        if (newChatMessage && !premiumManager.canSendMessageInVCChatOnVCGeneration(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to send a message when a temporary VC is created")
        }
    }
}
