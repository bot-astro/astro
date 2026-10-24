package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.GuildPermissionsHelper
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.GuildProfileUpdateBody
import space.astro.api.central.models.requests.GuildSettingsUpdateBody
import space.astro.api.central.models.responses.DiscordUserGuild
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.api.bot.request.GuildProfileUpdateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.util.*

@RestController
@Tag(name = "Guild settings")
class GuildSettingsController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val temporaryVCRepository: TemporaryVCRepository,
) {
    @Operation(summary = "Get guild settings")
    @GetMapping(CentralApiEndpoint.GUILD_SETTINGS)
    @CanManageGuildMiddleware
    fun getGuildSettings(
        @PathVariable guildId: String,
        @RequestAttribute(GuildPermissionsHelper.MANAGEABLE_DISCORD_GUILD_REQUEST_ATTRIBUTE)
        discordGuild: DiscordUserGuild,
    ): ResponseEntity<GuildSettingsEntity> {
        try {
            botApiClient.getGuild(guildId)
        } catch (_: ANotFoundException) {
            throw AException(
                httpStatusCode = 404,
                errorCode = AErrorCode.BOT_NOT_IN_GUILD,
                message = "Astro is not in guild with ID $guildId",
            )
        }

        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: guildSettingsRepository.createNewGuildSettings(guildId, discordGuild.preferredLocale)

        return ResponseEntity.ok(guildSettings)
    }

    @Operation(summary = "Update guild settings")
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS)
    @CanManageGuildMiddleware
    fun updateGuildSettings(
        @PathVariable guildId: String,
        @RequestBody newGuildSettings: GuildSettingsUpdateBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        guildSettings.apply {
            allowMissingAdminPerm = newGuildSettings.allowMissingAdminPerms
            locale = Locale.forLanguageTag(newGuildSettings.locale).toLanguageTag()
        }
        guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        summary = "Update bot profile",
        description = "Any null value will reset the corresponding field to the default value",
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_PROFILE)
    @CanManageGuildMiddleware
    fun updateGuildProfile(
        @PathVariable guildId: String,
        @RequestBody newProfile: GuildProfileUpdateBody,
    ): ResponseEntity<Void> {
        val guildSettings =
            guildSettingsRepository.findByIdOrNull(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        guildSettings.nickname = newProfile.nickname
        guildSettings.bio = newProfile.bio
        guildSettings.avatarUrl = newProfile.avatarUrl
        guildSettings.bannerUrl = newProfile.bannerUrl

        guildSettingsRepository.save(guildSettings)

        botApiClient.updateGuildProfile(
            guildId,
            GuildProfileUpdateBotApiRequest(
                nickname = newProfile.nickname,
                bio = newProfile.bio,
                avatarUrl = newProfile.avatarUrl,
                bannerUrl = newProfile.bannerUrl,
            ),
        )

        return ResponseEntity.ok().build()
    }

    @Operation(summary = "Clear temporary VCs cache")
    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_VC_CACHE)
    @CanManageGuildMiddleware
    fun clearVcsCache(
        @PathVariable guildId: String,
    ): ResponseEntity<Void> {
        temporaryVCRepository.deleteAll(guildId)
        return ResponseEntity.noContent().build()
    }
}
