package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
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
        summary = "Get guild errors",
        description = "Errors are described in the locale of the guild",
    )
    @GetMapping(CentralApiEndpoint.GUILD_ERRORS)
    @CanManageGuildMiddleware
    fun getGuildErrors(
        @PathVariable("guildId") guildId: String,
        @RequestParam(value = "from_timestamp", required = false) fromTimestamp: Long?,
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

    @Operation(summary = "Clear guild errors")
    @DeleteMapping(CentralApiEndpoint.GUILD_ERRORS)
    @CanManageGuildMiddleware
    fun clearGuildErrors(
        @PathVariable("guildId") guildId: String,
    ): ResponseEntity<Unit> {
        configurationErrorRepository.deleteAllByGuildId(guildId)
        return ResponseEntity.noContent().build()
    }
}
