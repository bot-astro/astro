package space.astro.api.central.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.repositories.ConfigurationErrorRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.time.Duration

@RestController
class GuildsErrorsController(
    private val configurationErrorRepository: ConfigurationErrorRepository
) {
    @GetMapping(CentralApiEndpoint.GUILD_ERRORS)
    fun getGuildErrors(
        @PathVariable("guildId") guildId: String,
        @RequestParam(value = "from_timestamp", required = false) fromTimestamp: Long?,
    ) : ResponseEntity<List<ConfigurationErrorEntity>> {
        val sevenDaysAgo = System.currentTimeMillis() - Duration.ofDays(7).toMillis()
        val filterTimestamp = fromTimestamp?.coerceAtLeast(sevenDaysAgo) ?: sevenDaysAgo
        val errors = configurationErrorRepository.findAllByTimestampGreaterThanEqual(filterTimestamp)

        return ResponseEntity.ok(errors)
    }
}