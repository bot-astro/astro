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
    @Operation(
        operationId = "discord.constants.listChannelPermissions",
        summary = "Get channel related permissions",
        description =
            "Lists the Discord permissions that apply to channels, with their ids. " +
                "The ids are the values accepted by `owner_permission_ids` of a generator.",
    )
    @GetMapping(CentralApiEndpoint.DISCORD_CHANNEL_RELATED_PERMISSIONS)
    fun getChannelRelatedPermissions(): ResponseEntity<List<DiscordPermissionMeta>> =
        ResponseEntity.ok(DiscordPermissionMeta.channelsRelatedPermissions)
}
