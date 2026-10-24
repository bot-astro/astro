package space.astro.support.bot.api.controllers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.utils.api.SupportBotApiEndpoint
import space.astro.support.bot.components.PremiumRoleService

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping
@Tag(name = "Premium Role")
class PremiumRoleController(
    private val premiumRoleService: PremiumRoleService,
) {
    @Operation(
        summary = "Add premium role",
        description = "Gives the premium role of the support server to the user, 404 if the user is not in the support server",
        operationId = "addPremiumRole",
    )
    @PutMapping(SupportBotApiEndpoint.PREMIUM_ROLE_USER)
    suspend fun addPremiumRole(
        @PathVariable userId: String,
    ): ResponseEntity<Void> {
        premiumRoleService.addPremiumRole(userId)

        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Remove premium role",
        description = "Removes the premium role of the support server from the user, 404 if the user is not in the support server",
        operationId = "removePremiumRole",
    )
    @DeleteMapping(SupportBotApiEndpoint.PREMIUM_ROLE_USER)
    suspend fun removePremiumRole(
        @PathVariable userId: String,
    ): ResponseEntity<Void> {
        premiumRoleService.removePremiumRole(userId)

        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(AException::class)
    fun handleException(e: AException): ResponseEntity<String> {
        if (e.httpStatusCode >= 500) {
            log.error(e) { "Premium role request failed" }
        }

        return ResponseEntity.status(e.httpStatusCode).body(e.message)
    }
}
