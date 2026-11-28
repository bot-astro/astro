package space.astro.api.central.controllers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.components.OpenApiConfiguration
import space.astro.api.central.models.requests.VoteTrackerWebhookBody
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.properties.api_central.CentralApiProperties
import space.astro.shared.core.repositories.redis.VoteStatusRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger { }

/**
 * Used when a vote has no expiration
 */
private val DEFAULT_VOTE_DURATION = Duration.ofHours(12)

@RestController
@Tag(name = "Vote Tracker")
class VoteTrackerController(
    private val centralApiProperties: CentralApiProperties,
    private val voteStatusRepository: VoteStatusRepository,
    private val jsonMapper: JsonMapper,
) {
    @Operation(
        operationId = "webhooks.voteTracker.receiveEvent",
        tags = [OpenApiConfiguration.WEBHOOKS_TAG],
        extensions = [Extension(properties = [ExtensionProperty(name = "x-forge-skip", value = "true", parseValue = true)])],
        summary = "Vote Tracker forwarding webhook",
        description = "Called by Vote Tracker, authenticated with the HMAC signature of the body",
    )
    @SecurityRequirements
    @PostMapping(CentralApiEndpoint.VOTE_TRACKER_EVENTS)
    fun receiveEvent(
        @RequestHeader("X-VT-Timestamp", required = false) timestamp: String?,
        @RequestHeader("X-VT-Signature", required = false) signature: String?,
        @RequestBody rawBody: String,
    ): ResponseEntity<Void> {
        if (timestamp == null || signature == null || !isSignatureValid(timestamp, signature, rawBody)) {
            throw AUnauthorizedException("Invalid Vote Tracker signature")
        }

        val body = jsonMapper.readValue(rawBody, VoteTrackerWebhookBody::class.java)
        val data = body.data

        // other event types are subscribed from the dashboard only by mistake, acknowledge them so they are not retried
        if (body.eventType != "VOTE_CREATED" || data?.userPlatformId == null) {
            return ResponseEntity.noContent().build()
        }

        if (data.test) {
            log.info { "Ignored Vote Tracker test vote of user ${data.userPlatformId}" }
            return ResponseEntity.noContent().build()
        }

        val expiresAt = data.voteExpiresAt?.let(Instant::parse) ?: Instant.now().plus(DEFAULT_VOTE_DURATION)
        voteStatusRepository.save(data.userPlatformId, expiresAt)

        return ResponseEntity.noContent().build()
    }

    /**
     * The signature is the hex HMAC-SHA256 of `{timestamp}.{raw body}`.
     *
     * The timestamp age is not checked: retries can be delivered up to 30 minutes later,
     * and replaying a vote is harmless since its expiration is part of the signed body.
     */
    private fun isSignatureValid(
        timestamp: String,
        signature: String,
        rawBody: String,
    ): Boolean {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(centralApiProperties.voteTrackerWebhookSecret.toByteArray(), "HmacSHA256"))
        val expected = mac.doFinal("$timestamp.$rawBody".toByteArray()).joinToString("") { "%02x".format(it) }

        return MessageDigest.isEqual(expected.toByteArray(), signature.lowercase().toByteArray())
    }
}
