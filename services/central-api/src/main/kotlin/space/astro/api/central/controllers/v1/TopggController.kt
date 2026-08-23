package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.api.central.models.topgg.TopggWebhookEvent
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.shared.core.daos.VoteDao


@RestController
@Tag(
    name = "topgg",
    description = "handling of all topgg webhooks"
)
class TopggController(
    val voteDao: VoteDao
) {
    @PostMapping(CentralApiRoutes.Topgg.EVENT)
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "webhook handled correctly"
            )
        ]
    )
    suspend fun handleWebhookEvent(
        @RequestBody event: TopggWebhookEvent
    ): ResponseEntity<*> {
        voteDao.save(event.userId, System.currentTimeMillis(), 43200)

        return ResponseEntity.ok().build<Any>()
    }
}