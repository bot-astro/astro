package space.astro.api.central.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
class GuildSettingsController {
    @GetMapping(CentralApiEndpoint.GUILD_SETTINGS)
    fun getGuildSettings(
        @PathVariable guildId: String,
    ): ResponseEntity<>
}