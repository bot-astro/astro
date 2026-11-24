package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.responses.ConfigurationErrorResponse
import space.astro.shared.core.lang.L
import space.astro.shared.core.repositories.mongo.ConfigurationErrorRepository
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.time.Duration

@RestController
@Tag(name = "Guild errors")
class GuildErrorsController(
    private val configurationErrorRepository: ConfigurationErrorRepository,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @Operation(
        operationId = "guilds.errors.list",
        summary = "Get guild errors",
        description =
            "Lists the configuration errors Astro hit in the guild in the last 7 days (for example missing permissions " +
                "when creating a temporary VC), newest data last. Each has a `description` in the language of the guild " +
                "and a `guide` that explains how to fix it. Use it to find out why Astro is not working as expected.",
    )
    @GetMapping(CentralApiEndpoint.GUILD_ERRORS)
    @CanManageGuildMiddleware
    fun getGuildErrors(
        @PathVariable("guildId") guildId: String,
        @Parameter(
            description = "Only return the errors from this time on, in epoch milliseconds. Older than 7 days is ignored.",
            example = "1767225600000",
        ) @RequestParam(value = "from_timestamp", required = false) fromTimestamp: Long?,
    ): ResponseEntity<List<ConfigurationErrorResponse>> {
        val sevenDaysAgo = System.currentTimeMillis() - Duration.ofDays(7).toMillis()
        val filterTimestamp = fromTimestamp?.coerceAtLeast(sevenDaysAgo) ?: sevenDaysAgo
        val errors =
            configurationErrorRepository.findAllByGuildIdAndTimestampGreaterThanEqual(
                guildId,
                filterTimestamp,
            )

        val responseLocale =
            guildSettingsRepository
                .findByGuildID(guildId)
                ?.locale
                ?.let { L.closestSupportedLocale(it) }
                ?: L.default

        return ResponseEntity.ok(errors.map { ConfigurationErrorResponse.from(it, responseLocale) })
    }

    @Operation(
        operationId = "guilds.errors.clear",
        summary = "Clear guild errors",
        description = "Deletes all the stored configuration errors of the guild, for example after fixing them.",
        extensions = [
            Extension(
                properties = [
                    ExtensionProperty(
                        name = "x-forge-require-confirmation",
                        value = "This deletes all the stored configuration errors of the guild.",
                    ),
                ],
            ),
        ],
    )
    @ApiResponse(responseCode = "204", description = "The errors were deleted.")
    @DeleteMapping(CentralApiEndpoint.GUILD_ERRORS)
    @CanManageGuildMiddleware
    fun clearGuildErrors(
        @PathVariable("guildId") guildId: String,
    ): ResponseEntity<Unit> {
        configurationErrorRepository.deleteAllByGuildId(guildId)
        return ResponseEntity.noContent().build()
    }
}
