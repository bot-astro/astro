package space.astro.api.central.controllers

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.requests.GuildSettingsUpdateBody
import space.astro.api.central.services.DiscordUserGuildsPersistenceService
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.util.Locale

@RestController
class GuildSettingsController(
    private val discordUserGuildsPersistenceService: DiscordUserGuildsPersistenceService,
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @GetMapping(CentralApiEndpoint.GUILD_SETTINGS)
    fun getGuildSettings(
        @PathVariable guildId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal
    ): ResponseEntity<GuildSettingsEntity> {
        val discordGuild = discordUserGuildsPersistenceService.getUserGuild(authPrincipal.userId, guildId)
            ?: discordUserGuildsPersistenceService.fetchFromDiscord(authPrincipal.userId, authPrincipal.userDiscordToken)
                .firstOrNull { it.id == guildId }
            ?: throw ANotFoundException("Guild with ID $guildId not found in the user's Discord guilds")

        if (!discordGuild.canManage) {
            throw AUnauthorizedException("User is not allowed to manage guild with ID $guildId")
        }

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
    fun updateGuildSettings(
        @PathVariable guildId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
        @RequestBody newGuildSettings: GuildSettingsUpdateBody
    ): ResponseEntity<GuildSettingsEntity> {
        val discordGuild = discordUserGuildsPersistenceService.getUserGuild(authPrincipal.userId, guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found in the user's Discord guilds")

        if (!discordGuild.canManage) {
            throw AUnauthorizedException("User is not allowed to manage guild with ID $guildId")
        }

        val validation = newGuildSettings.validate()
        if (!validation.isValid) {
            throw ABadRequestException("Invalid guild settings provided")
        }

        val guildData = guildSettingsRepository.findByIdOrNull(guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        guildData.apply {
            allowMissingAdminPerm = newGuildSettings.allowMissingAdminPerms
            locale = Locale.forLanguageTag(newGuildSettings.locale).toLanguageTag()
        }
        guildSettingsRepository.save(guildData)

        return ResponseEntity.ok(guildData)
    }
}