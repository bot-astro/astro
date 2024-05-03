package space.astro.api.central.controllers.dashboard

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings
import space.astro.shared.core.daos.TemporaryVCDao

@RestController
@Tag(name = "dashboard-data")
class DashboardGuildVcController(
    private val temporaryVCDao: TemporaryVCDao
) {
    @DeleteMapping(Mappings.Dashboard.GUILD_TEMPORARY_VOICE_CHANNELS_CACHE)
    suspend fun clearTemporaryVoiceChannelsData(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        temporaryVCDao.deleteAll(guildID)
        return ResponseEntity.noContent().build<Any>()
    }
}