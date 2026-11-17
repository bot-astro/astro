package space.astro.api.central.controllers.guild_settings

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.UpdatesChannelUpdateBody
import space.astro.api.central.models.responses.UpdatesChannelResponse
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.api.bot.request.UpdatesChannelUpdateBotApiRequest
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Updates channel")
class GuildUpdatesChannelController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @Operation(summary = "Get updates channel", description = "Both fields are null if the updates channel is disabled")
    @GetMapping(CentralApiEndpoint.GUILD_UPDATES_CHANNEL)
    @CanManageGuildMiddleware
    fun getUpdatesChannel(
        @PathVariable guildId: String,
    ): ResponseEntity<UpdatesChannelResponse> {
        val guildSettings = guildSettingsRepository.findByGuildID(guildId)
        return ResponseEntity.ok(UpdatesChannelResponse.from(guildSettings?.updatesChannel))
    }

    @Operation(
        summary = "Update updates channel",
        description =
            "Moves the updates channel to another channel, creates a new one or disables it. " +
                "Setting the channel that is already in use sets it up again",
    )
    @PutMapping(CentralApiEndpoint.GUILD_UPDATES_CHANNEL)
    @CanManageGuildMiddleware
    fun updateUpdatesChannel(
        @PathVariable guildId: String,
        @RequestBody body: UpdatesChannelUpdateBody,
    ): ResponseEntity<UpdatesChannelResponse> {
        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val updatesChannel =
            botApiClient.updateUpdatesChannel(
                guildId,
                UpdatesChannelUpdateBotApiRequest(
                    channelId = body.channelId,
                    createChannel = body.createChannel,
                    current = guildSettings.updatesChannel,
                ),
            )

        guildSettings.updatesChannel = updatesChannel
        guildSettingsRepository.save(guildSettings)

        return ResponseEntity.ok(UpdatesChannelResponse.from(updatesChannel))
    }
}
