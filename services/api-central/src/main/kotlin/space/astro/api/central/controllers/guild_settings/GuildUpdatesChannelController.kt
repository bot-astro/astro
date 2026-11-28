package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.UpdatesChannelUpdateBody
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.api.bot.request.UpdatesChannelUpdateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Updates channel settings")
class GuildUpdatesChannelController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @Operation(
        operationId = "guilds.updatesChannel.update",
        summary = "Update updates channel",
        description =
            "Moves the updates channel (where Astro posts announcements and setup alerts) to another text channel, " +
                "creates a new one with `create_channel`, or disables it with a null `channel_id`. " +
                "Setting the channel that is already in use sets it up again. Returns the updated guild settings, " +
                "`updates_channel_id` is the new channel.",
    )
    @PutMapping(CentralApiEndpoint.GUILD_SETTINGS_UPDATES_CHANNEL)
    @CanManageGuildMiddleware
    fun updateUpdatesChannel(
        @PathVariable guildId: String,
        @RequestBody body: UpdatesChannelUpdateBody,
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val updatesChannel =
            botApiClient.updateUpdatesChannel(
                guildId,
                UpdatesChannelUpdateBotApiRequest(
                    channelId = body.channelId,
                    createChannel = body.createChannel,
                ),
            )

        guildSettings.updatesChannel = updatesChannel
        guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok(guildSettings)
    }
}
