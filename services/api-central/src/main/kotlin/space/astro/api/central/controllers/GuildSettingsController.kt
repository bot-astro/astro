package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.GuildPermissionsHelper
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.GeneratorCreateBody
import space.astro.api.central.models.requests.GuildProfileUpdateBody
import space.astro.api.central.models.requests.GuildSettingsUpdateBody
import space.astro.api.central.models.responses.DiscordUserGuild
import space.astro.api.central.services.MediaStorageService
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.api.bot.request.ChannelCreateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.util.*

@RestController
class GuildSettingsController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val mediaStorageService: MediaStorageService
) {
    @GetMapping(CentralApiEndpoint.GUILD_SETTINGS)
    @CanManageGuildMiddleware
    fun getGuildSettings(
        @PathVariable guildId: String,
        @RequestAttribute(GuildPermissionsHelper.MANAGEABLE_DISCORD_GUILD_REQUEST_ATTRIBUTE)
        discordGuild: DiscordUserGuild
    ): ResponseEntity<GuildSettingsEntity> {
        try {
            botApiClient.getGuild(guildId)
        } catch (_: ANotFoundException) {
            throw AException(
                httpStatusCode = 404,
                errorCode = AErrorCode.BOT_NOT_IN_GUILD,
                message = "Astro is not in guild with ID $guildId"
            )
        }

        val guildSettings = guildSettingsRepository.findByIdOrNull(guildId)
            ?: guildSettingsRepository.createNewGuildSettings(guildId, discordGuild.preferredLocale)

        return ResponseEntity.ok(guildSettings)
    }

    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS)
    @CanManageGuildMiddleware
    fun updateGuildSettings(
        @PathVariable guildId: String,
        @RequestBody newGuildSettings: GuildSettingsUpdateBody
    ): ResponseEntity<GuildSettingsEntity> {
        val guildData = guildSettingsRepository.findByIdOrNull(guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        guildData.apply {
            allowMissingAdminPerm = newGuildSettings.allowMissingAdminPerms
            locale = Locale.forLanguageTag(newGuildSettings.locale).toLanguageTag()
        }
        guildSettingsRepository.save(guildData)

        return ResponseEntity.ok(guildData)
    }

    @Operation(
        summary = "Updates the bot profile for a guild",
        description = "Any null value will reset the corresponding field to the default value"
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_PROFILE)
    fun updateGuildProfile(
        @PathVariable guildId: String,
        @RequestBody newProfile: GuildProfileUpdateBody
    ): ResponseEntity<Void> {
        // 1. store in guild settings
        // 2. update in Discord

        return ResponseEntity.ok().build()
    }

    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATORS)
    @CanManageGuildMiddleware
    fun createGenerator(
        @PathVariable guildId: String,
        @RequestBody generatorCreateBody: GeneratorCreateBody
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings = guildSettingsRepository.findByIdOrNull(guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val generatorDiscordChannel = botApiClient.createChannel(
            guildId,
            ChannelCreateBotApiRequest(
                name = generatorCreateBody.name,
                categoryId = generatorCreateBody.categoryId,
                channelType = ChannelCreateBotApiRequest.ChannelCreateType.VOICE
            )
        )

        val generatorSettings = GeneratorSettings(
            id = generatorDiscordChannel.id,
            defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default"),
            defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default"),
            defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default"),
        )

        guildSettings.generators.add(generatorSettings)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATORS)
    @CanManageGuildMiddleware
    fun updateGenerator(
        @PathVariable guildId: String,
        @RequestBody updatedGeneratorSettings: GeneratorSettings
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings = guildSettingsRepository.findByIdOrNull(guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val generatorIndex = guildSettings.generators.indexOfFirst { it.id == updatedGeneratorSettings.id }
            .takeIf { it != -1 }
            ?: throw ANotFoundException("Generator with ID ${updatedGeneratorSettings.id} not found for guild with id $guildId")

        guildSettings.generators[generatorIndex] = updatedGeneratorSettings
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATOR)
    fun deleteGenerator(
        @PathVariable guildId: String,
        @PathVariable generatorId: String
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings = guildSettingsRepository.findByIdOrNull(guildId)
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
}