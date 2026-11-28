package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.MergePatchHelper
import space.astro.api.central.components.SupportsDryRun
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.GuildProfileUpdateBody
import space.astro.api.central.models.requests.GuildSettingsUpdateBody
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.api.bot.request.GuildProfileUpdateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import tools.jackson.databind.JsonNode
import java.util.*

@RestController
@Tag(name = "Guild settings")
class GuildSettingsController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val mergePatchHelper: MergePatchHelper,
) {
    @Operation(
        operationId = "guilds.settings.get",
        summary = "Get guild settings",
        description =
            "Returns the full configuration of Astro in the guild: language, bot profile, premium status, " +
                "generators, interfaces, voice roles (connections), templates and entitlements. " +
                "Creates the default settings if the guild has none yet. " +
                "Read this first to learn the ids needed by the other guild endpoints. " +
                "Fails with 404 BOT_NOT_IN_GUILD if Astro was not added to the guild.",
    )
    @GetMapping(CentralApiEndpoint.GUILD_SETTINGS)
    @CanManageGuildMiddleware
    fun getGuildSettings(
        @PathVariable guildId: String,
    ): ResponseEntity<GuildSettingsEntity> {
        val discordGuild =
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
            guildSettingsRepository.findByGuildID(guildId)
                ?: guildSettingsRepository.createNewGuildSettings(guildId, discordGuild.preferredLocale)

        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.settings.replace",
        summary = "Replace guild settings",
        description =
            "Sets the language of the bot in the guild and whether Astro keeps working when it lacks the Administrator permission. " +
                "Both fields are required and replace the current values. Returns the updated guild settings. " +
                "Prefer the PATCH endpoint, which changes only the fields you send.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-forge-skip", value = "true", parseValue = true)])],
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS)
    @CanManageGuildMiddleware
    fun updateGuildSettings(
        @PathVariable guildId: String,
        @RequestBody newGuildSettings: GuildSettingsUpdateBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        guildSettings.apply {
            allowMissingAdminPerm = newGuildSettings.allowMissingAdminPerms
            locale = Locale.forLanguageTag(newGuildSettings.locale).toLanguageTag()
        }
        guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.settings.update",
        summary = "Update guild settings",
        description =
            "Changes the language of the bot in the guild (`locale`) and/or whether Astro keeps working when it lacks the " +
                "Administrator permission (`allow_missing_admin_perms`). Only the fields present in the body change. " +
                "Returns the updated guild settings. Idempotent.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-merge-patch-of", value = "GuildSettingsUpdateBody")])],
    )
    @PatchMapping(CentralApiEndpoint.GUILD_SETTINGS)
    @CanManageGuildMiddleware
    @SupportsDryRun
    fun patchGuildSettings(
        @PathVariable guildId: String,
        @Parameter(description = "When true nothing is saved and the guild settings the update would produce are returned.")
        @RequestParam(defaultValue = "false") dryRun: Boolean,
        @RequestBody patch: JsonNode,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        val updated =
            mergePatchHelper.apply(
                GuildSettingsUpdateBody(guildSettings.allowMissingAdminPerm, guildSettings.locale),
                patch,
                GuildSettingsUpdateBody::class.java,
            )

        guildSettings.apply {
            allowMissingAdminPerm = updated.allowMissingAdminPerms
            locale = Locale.forLanguageTag(updated.locale).toLanguageTag()
        }
        if (!dryRun) guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.settings.replaceProfile",
        summary = "Replace bot profile",
        description =
            "Sets the nickname, bio, avatar and banner that Astro shows in this guild. " +
                "All four fields are replaced: a missing or null field resets it to the default Astro value, " +
                "so send the current values of the fields that must stay. " +
                "Avatar and banner are image urls, upload them with the media endpoints first.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-forge-skip", value = "true", parseValue = true)])],
    )
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_PROFILE)
    @CanManageGuildMiddleware
    fun updateGuildProfile(
        @PathVariable guildId: String,
        @RequestBody newProfile: GuildProfileUpdateBody,
    ): ResponseEntity<Void> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        botApiClient.updateGuildProfile(
            guildId,
            GuildProfileUpdateBotApiRequest(
                nickname = newProfile.nickname,
                bio = newProfile.bio,
                avatarUrl = newProfile.avatarUrl,
                bannerUrl = newProfile.bannerUrl,
            ),
        )

        guildSettings.nickname = newProfile.nickname
        guildSettings.bio = newProfile.bio
        guildSettings.avatarUrl = newProfile.avatarUrl
        guildSettings.bannerUrl = newProfile.bannerUrl

        guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok().build()
    }

    @Operation(
        operationId = "guilds.settings.updateProfile",
        summary = "Update bot profile",
        description =
            "Changes the nickname, bio, avatar and/or banner that Astro shows in this guild. Only the fields present " +
                "in the body change, a null field resets it to the default Astro value. " +
                "Avatar and banner are image urls, upload them with the media endpoints first. " +
                "Returns the updated guild settings. Idempotent.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-merge-patch-of", value = "GuildProfileUpdateBody")])],
    )
    @PatchMapping(CentralApiEndpoint.GUILD_SETTINGS_PROFILE)
    @CanManageGuildMiddleware
    @SupportsDryRun
    fun patchGuildProfile(
        @PathVariable guildId: String,
        @Parameter(description = "When true nothing is saved and the guild settings the update would produce are returned.")
        @RequestParam(defaultValue = "false") dryRun: Boolean,
        @RequestBody patch: JsonNode,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found Astro database")

        val updated =
            mergePatchHelper.apply(
                GuildProfileUpdateBody(guildSettings.nickname, guildSettings.bio, guildSettings.avatarUrl, guildSettings.bannerUrl),
                patch,
                GuildProfileUpdateBody::class.java,
            )

        if (!dryRun) {
            botApiClient.updateGuildProfile(
                guildId,
                GuildProfileUpdateBotApiRequest(
                    nickname = updated.nickname,
                    bio = updated.bio,
                    avatarUrl = updated.avatarUrl,
                    bannerUrl = updated.bannerUrl,
                ),
            )
        }

        guildSettings.nickname = updated.nickname
        guildSettings.bio = updated.bio
        guildSettings.avatarUrl = updated.avatarUrl
        guildSettings.bannerUrl = updated.bannerUrl

        if (!dryRun) guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @Operation(
        operationId = "guilds.settings.clearVcCache",
        summary = "Clear temporary VCs cache",
        description =
            "Forgets which voice channels Astro tracks as temporary VCs in the guild. " +
                "Use it when temporary VCs got stuck or out of sync, for example after being deleted manually. " +
                "The VCs are not deleted from Discord.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This stops Astro from tracking the current temporary VCs of the guild.",
                    ),
                ],
            ),
        ],
    )
    @ApiResponse(responseCode = "204", description = "The cache was cleared.")
    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_VC_CACHE)
    @CanManageGuildMiddleware
    fun clearVcsCache(
        @PathVariable guildId: String,
    ): ResponseEntity<Void> {
        temporaryVCRepository.deleteAll(guildId)
        return ResponseEntity.noContent().build()
    }
}
