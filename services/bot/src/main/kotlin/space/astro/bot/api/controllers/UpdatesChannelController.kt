package space.astro.bot.api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.bot.components.UpdatesChannelManager
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.api.bot.request.UpdatesChannelUpdateBotApiRequest
import space.astro.shared.core.models.api.bot.response.UpdatesChannelBotApiResponse
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.BotApiEndpoint

@RestController
@RequestMapping
@Tag(name = "Updates channel")
class UpdatesChannelController(
    private val shardManager: ShardManager,
    private val updatesChannelManager: UpdatesChannelManager,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @Operation(
        summary = "Update the updates channel",
        description =
            "Moves the updates channel to the given channel, creates a new one or disables it. " +
                "The webhooks of the previous channel are removed only after the new ones work. " +
                "Returns the new data to save, null if the updates channel has been disabled",
        operationId = "updateUpdatesChannel",
    )
    @PutMapping(BotApiEndpoint.DISCORD_GUILD_UPDATES_CHANNEL)
    fun updateUpdatesChannel(
        @PathVariable guildId: String,
        @RequestBody request: UpdatesChannelUpdateBotApiRequest,
    ): ResponseEntity<UpdatesChannelBotApiResponse> {
        val guild =
            shardManager.getGuildById(guildId)
                ?: throw ANotFoundException("Guild not found")

        val locale = guildSettingsRepository.findByGuildID(guildId).supportedLocale

        try {
            val updatesChannel =
                runBlocking(Dispatchers.IO) {
                    when {
                        request.createChannel ->
                            updatesChannelManager.createAndConfigure(
                                guild = guild,
                                category = null,
                                locale = locale,
                                reason = L.t(locale, "reason.dashboard.updates_channel_create"),
                                current = request.current,
                            )

                        request.channelId != null -> {
                            val channel =
                                guild.getTextChannelById(request.channelId)
                                    ?: throw ANotFoundException("Text channel with id ${request.channelId} not found")

                            updatesChannelManager.configure(guild, channel, locale, request.current)
                        }

                        else -> {
                            updatesChannelManager.disable(guild, request.current)
                            null
                        }
                    }
                }

            return ResponseEntity.ok(UpdatesChannelBotApiResponse(updatesChannel))
        } catch (e: AException) {
            throw e
        } catch (e: ErrorResponseException) {
            throw ABadRequestException(e.message ?: "Error from Discord API", e)
        } catch (e: InsufficientPermissionException) {
            throw ABadRequestException("Insufficient permissions to use the channel as the updates channel", e)
        } catch (e: Exception) {
            throw AUnknownException("An unexpected error occurred", e)
        }
    }
}
