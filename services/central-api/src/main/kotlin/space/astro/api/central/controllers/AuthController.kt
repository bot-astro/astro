package space.astro.api.central.controllers

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.daos.AuthedUsersDao
import space.astro.api.central.models.OAuth2AuthorizationResponseDto
import space.astro.api.central.services.DiscordUserService
import space.astro.api.central.services.DiscordUserTokenFetchService
import space.astro.api.central.services.DiscordUserTokenPersistenceService
import space.astro.api.central.services.WebSessionService

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/auth")
class AuthController(
    val discordUserTokenFetchService: DiscordUserTokenFetchService,
    val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    val discordUserService: DiscordUserService,
    val authedUsersDao: AuthedUsersDao,
    val webSessionService: WebSessionService
) {

    @GetMapping("/id/{code}")
    suspend fun authorizeDiscord(@PathVariable code: String): ResponseEntity<*> {
        log.info { "Authorizing discord user with code $code" }

        // fetch credentials + user from discord
        val authorizationWrapper = discordUserTokenFetchService.fetchCredentialsFromCode(code)

        val user = authorizationWrapper.user

        val sessionToken = webSessionService.createSession(user.id)

        val response = OAuth2AuthorizationResponseDto(
            sessionToken,
            user
        )

        log.info { "Successfully authorized discord user with id ${user.id} - response $response" }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/user/{id}")
    suspend fun getAuthenticatedDiscordUser(@PathVariable id: String): ResponseEntity<*> {
        val authedUser = authedUsersDao.getAuthedUser(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)

        try {
            val discordUser = discordUserService.fetchSelfUser(authedUser.discordAuthTokenInfo.accessToken)
            return ResponseEntity.ok(discordUser)
        } catch (e: Throwable) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @GetMapping("/user/delete/{id}")
    suspend fun logoutDiscord(
        @RequestHeader("Authorization") sessionToken: String,
        @PathVariable id: String
    ): ResponseEntity<*> {
        discordUserTokenPersistenceService.deleteCredentials(id)
        webSessionService.deleteSessions(id)
        return ResponseEntity.ok(null)
    }
}