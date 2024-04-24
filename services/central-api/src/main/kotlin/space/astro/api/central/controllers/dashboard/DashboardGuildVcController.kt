package space.astro.api.central.controllers.dashboard

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings

@RestController
class DashboardGuildVcController(

) {
    @DeleteMapping(Mappings.Dashboard.GUILD_TEMPORARY_VOICE_CHANNELS_CACHE)
    suspend fun clearTemporaryVoiceChannelsData(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {
        /*
         TODO:
          - clear
          - respond with ok
         */
    }
}