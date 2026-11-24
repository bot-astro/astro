package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
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
    @Operation(
        operationId = "guilds.generators.create",
        summary = "Create generator",
        description =
            "Creates a voice channel in the guild and registers it as a generator: when a member joins it, " +
                "Astro creates a temporary VC for them. The new generator gets the default settings, " +
                "change them with the update endpoint. Returns the updated guild settings, the new generator is the " +
                "last of `generators`. " +
                "Fails with 403 PREMIUM_REQUIRED if the guild reached the generators limit of a free server.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This creates a new voice channel in the Discord guild.",
                    ),
                ],
            ),
        ],
    )
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
                category = generatorCreateBody.categoryId,
                chatCategory = generatorCreateBody.categoryId,
                waitingCategory = generatorCreateBody.categoryId,
                // the variables are kept as they are, they get replaced when the temporary VC is created
                defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default", "nickname" to "{nickname}"),
                defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default", "vc_name" to "{vc_name}"),
                defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default", "vc_name" to "{vc_name}"),
            )

        guildSettings.generators.add(generatorSettings)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.generators.update",
        summary = "Update generator",
        description =
            "Replaces ALL the settings of a generator with the request body, it is not a partial update: " +
                "get the current generator from the guild settings, change the fields you need and send the whole object back. " +
                "The `id` in the body is ignored, the path decides which generator is updated. " +
                "Settings that need premium fail with 403 PREMIUM_REQUIRED only when they are newly set, " +
                "values already stored are kept. Returns the updated guild settings. Idempotent.",
    )
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

    @Operation(
        operationId = "guilds.generators.delete",
        summary = "Delete generator",
        description =
            "Removes the generator from the guild settings and deletes its voice channel on Discord " +
                "(a failure to delete the channel is ignored). Temporary VCs it already created are not deleted. " +
                "Returns the updated guild settings.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This deletes the generator and its voice channel on Discord.",
                    ),
                ],
            ),
        ],
    )
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

        // DEPRECATED: -1 is a legacy value for an unset chat interface
        val newChatMessage =
            (updated.defaultChatText != null && updated.defaultChatText != current.defaultChatText) ||
                (updated.chatInterface?.let { it > -1 && it != current.chatInterface } == true)

        if (newChatMessage && !premiumManager.canSendMessageInVCChatOnVCGeneration(guildSettings)) {
            throw APremiumRequiredException("Upgrade the server to send a message when a temporary VC is created")
        }
    }
}
