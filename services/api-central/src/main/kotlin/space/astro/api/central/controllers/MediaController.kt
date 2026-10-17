package space.astro.api.central.controllers

import org.bson.types.ObjectId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import space.astro.api.central.models.requests.MediaCreateBody
import space.astro.api.central.services.MediaContentType
import space.astro.api.central.services.MediaStorageService
import space.astro.api.central.services.RateLimitingService
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.models.database.MediaEntity
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.properties.R2Properties
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.mongo.MediaRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import kotlin.math.max
import kotlin.time.Duration.Companion.nanoseconds

@Controller
class MediaController(
    private val mediaStorageService: MediaStorageService,
    private val mediaRepository: MediaRepository,
    private val rateLimitingService: RateLimitingService,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val r2Properties: R2Properties,
) {
    @PostMapping(CentralApiEndpoint.GUILD_MEDIA)
    fun uploadMedia(
        @PathVariable("guildId") guildId: String,
        @RequestPart("meta", required = true) mediaMeta: MediaCreateBody,
        @RequestPart("media", required = true) media: MultipartFile,
    ): ResponseEntity<*> {
        // daily rate limit
        val mediaRateLimitBucket = rateLimitingService.resolveMediaBucket(guildId)
        val probe = mediaRateLimitBucket.tryConsumeAndReturnRemaining(1)

        if (!probe.isConsumed) {
            val retryAfterSeconds = max(1, probe.nanosToWaitForRefill.nanoseconds.inWholeSeconds)
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", retryAfterSeconds.toString())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(
                    AErrorResponse(
                        code = AErrorCode.TOO_MANY_REQUESTS,
                        message = "You've hit the media upload limit for today, try again in $retryAfterSeconds seconds.",
                    ),
                )
        }

        // total media amount limit
        val mediaOfGuild = mediaRepository.findAllByGuildId(guildId)
        val mediaOfGuildCount = mediaOfGuild.size
        if (mediaOfGuildCount > MAX_MEDIA_PER_GUILD) {
            throw AException(
                httpStatusCode = HttpStatus.METHOD_NOT_ALLOWED.value(),
                errorCode = AErrorCode.INVALID_REQUEST,
                message = "You've uploaded too many media files.",
            )
        }

        // cleanup unused media
        if (mediaOfGuildCount % MEDIA_CLEANUP_INTERVAL == 0L) {
            val guildSettingsEntity =
                guildSettingsRepository.findByIdOrNull(guildId)
                    ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

            val usedMediaIds = getMediaIdsUsedByGuild(guildSettingsEntity)
            val outdatedMediaKeys =
                mediaOfGuild
                    .filter { it.id in (mediaOfGuild.map { media -> media.id }.toSet() - usedMediaIds) }
                    .map { it.key }

            if (outdatedMediaKeys.isNotEmpty()) {
                mediaStorageService.deleteAll(outdatedMediaKeys)
            }
        }

        // save uploaded media
        val mediaKey =
            mediaStorageService.upload(
                type = mediaMeta.mediaType,
                guildId = guildId,
                data = media.bytes,
                contentType =
                    MediaContentType.fromMime(
                        media.originalFilename?.substringAfterLast(".")
                            ?: throw ABadRequestException("Missing file name for media"),
                    ),
            )

        val mediaEntity =
            MediaEntity(
                guildId = guildId,
                bucket = r2Properties.bucketName,
                key = mediaKey,
                type = mediaMeta.mediaType,
            )

        mediaRepository.save(mediaEntity)

        return ResponseEntity.ok(mediaEntity)
    }

    private fun getMediaIdsUsedByGuild(guildSettings: GuildSettingsEntity): Set<ObjectId> {
        // TODO: updated once the interface model is correct
        return buildSet {
            for (i in guildSettings.interfaces) {
//                add(i.embedStyle.authorIconUrl)
            }
        }
    }

    private companion object {
        const val MAX_MEDIA_PER_GUILD = 200L
        const val MEDIA_CLEANUP_INTERVAL = 20L
    }
}
