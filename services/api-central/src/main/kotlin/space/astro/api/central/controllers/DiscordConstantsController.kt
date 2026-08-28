package space.astro.api.central.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.utils.DiscordPermissionMeta
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
class DiscordConstantsController {

    @GetMapping(CentralApiEndpoint.DISCORD_CHANNEL_RELATED_PERMISSIONS)
    suspend fun getChannelRelatedPermissions() : ResponseEntity<List<DiscordPermissionMeta>> {
        return ResponseEntity.ok(DiscordPermissionMeta.channelsRelatedPermissions)
    }
}