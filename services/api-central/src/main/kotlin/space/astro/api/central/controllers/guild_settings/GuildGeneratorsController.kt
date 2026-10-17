package space.astro.api.central.controllers.guild_settings

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.GeneratorCreateBody
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.api.bot.request.ChannelCreateBotApiRequest
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
class GuildGeneratorsController(
    private val botApiClient: BotApiClient,
    private val guildSettingsRepository: GuildSettingsRepository
) {
    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATORS)
    @CanManageGuildMiddleware
    fun createGenerator(
        @PathVariable guildId: String,
        @RequestBody generatorCreateBody: GeneratorCreateBody
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings = guildSettingsRepository.findByIdOrNull(guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val generatorDiscordChannel = botApiClient.createChannel(
            guildId,
            ChannelCreateBotApiRequest(
                name = generatorCreateBody.name,
                categoryId = generatorCreateBody.categoryId,
                channelType = ChannelCreateBotApiRequest.ChannelCreateType.VOICE
            )
        )

        val generatorSettings = GeneratorSettings(
            id = generatorDiscordChannel.id,
            defaultName = L.t(guildSettings.javaLocale, "temporary.vc.name.default"),
            defaultChatName = L.t(guildSettings.javaLocale, "temporary.chat.name.default"),
            defaultWaitingName = L.t(guildSettings.javaLocale, "temporary.waiting.name.default"),
        )

        guildSettings.generators.add(generatorSettings)
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @PostMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATOR)
    @CanManageGuildMiddleware
    fun updateGenerator(
        @PathVariable guildId: String,
        @PathVariable generatorId: String,
        @RequestBody updatedGeneratorSettings: GeneratorSettings
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings = guildSettingsRepository.findByIdOrNull(guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        val generatorIndex = guildSettings.generators.indexOfFirst { it.id == generatorId }
            .takeIf { it != -1 }
            ?: throw ANotFoundException("Generator with ID ${updatedGeneratorSettings.id} not found for guild with id $guildId")

        guildSettings.generators[generatorIndex] = updatedGeneratorSettings
        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }

    @DeleteMapping(CentralApiEndpoint.GUILD_SETTINGS_GENERATOR)
    fun deleteGenerator(
        @PathVariable guildId: String,
        @PathVariable generatorId: String
    ): ResponseEntity<GuildSettingsEntity> {
        val guildSettings = guildSettingsRepository.findByIdOrNull(guildId)
            ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

        guildSettings.generators.removeIf { it.id == generatorId }
        try {
            botApiClient.deleteChannel(guildId, generatorId)
        } catch (_: Exception) {
            // we don't care if it fails deleting on Discord to be fair
            // as the user might have already deleted it
        }

        guildSettingsRepository.save(guildSettings)
        return ResponseEntity.ok(guildSettings)
    }
}