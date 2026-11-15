package space.astro.bot.api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.bot.commands.setup.SetupManager
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.api.bot.response.DiscordGuildChannelBotApiResponse
import space.astro.shared.core.models.api.bot.response.SetupBotApiResponse
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.BotApiEndpoint

@RestController
@RequestMapping
@Tag(name = "Setup")
class SetupController(
    private val shardManager: ShardManager,
    private val setupManager: SetupManager,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    @Operation(
        summary = "Set up Astro",
        description =
            "Creates what the /setup command creates: a category with an interface and a generator inside it, " +
                "the caller is responsible for saving the returned interface and generator in the guild settings",
        operationId = "setup",
    )
    @PostMapping(BotApiEndpoint.DISCORD_GUILD_SETUP)
    fun setup(
        @PathVariable guildId: String,
    ): ResponseEntity<SetupBotApiResponse> {
        val guild =
            shardManager.getGuildById(guildId)
                ?: throw ANotFoundException("Guild not found")

        val guildSettings =
            guildSettingsRepository.findByGuildID(guildId)
                ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        if (!setupManager.hasRoomForSetup(guild)) {
            throw ABadRequestException("The server has reached the maximum number of channels, delete at least 4 channels to run the setup")
        }

        try {
            val locale = guildSettings.supportedLocale
            val setup =
                runBlocking(Dispatchers.IO) {
                    setupManager.createSetup(guild, guildSettings, locale, L.t(locale, "reason.dashboard.setup"))
                }

            return ResponseEntity.ok(
                SetupBotApiResponse(
                    category = setup.category.toResponse(),
                    interfaceChannel = setup.interfaceChannel.toResponse(),
                    generatorChannel = setup.generatorChannel.toResponse(),
                    interfaceData = setup.interfaceData,
                    generatorSettings = setup.generatorSettings,
                ),
            )
        } catch (e: AException) {
            throw e
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.MAX_CHANNELS) {
                throw ABadRequestException("The server has reached the maximum number of channels", e)
            }

            throw ABadRequestException(e.message ?: "Error from Discord API", e)
        } catch (e: InsufficientPermissionException) {
            throw ABadRequestException("Insufficient permissions to set up Astro", e)
        } catch (e: Exception) {
            throw AUnknownException("An unexpected error occurred", e)
        }
    }

    private fun GuildChannel.toResponse(): DiscordGuildChannelBotApiResponse {
        val parent = (this as? ICategorizableChannel)?.parentCategory

        return DiscordGuildChannelBotApiResponse(
            id = id,
            name = name,
            type = type.id,
            parentId = parent?.id,
            parentName = parent?.name,
        )
    }
}
