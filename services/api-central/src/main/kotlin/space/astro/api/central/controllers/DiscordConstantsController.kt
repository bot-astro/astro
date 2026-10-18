package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.utils.DiscordPermissionMeta
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Discord constants")
class DiscordConstantsController {
    @Operation(summary = "Get channel related permissions")
    @GetMapping(CentralApiEndpoint.DISCORD_CHANNEL_RELATED_PERMISSIONS)
    suspend fun getChannelRelatedPermissions(): ResponseEntity<List<DiscordPermissionMeta>> =
        ResponseEntity.ok(DiscordPermissionMeta.channelsRelatedPermissions)
}
