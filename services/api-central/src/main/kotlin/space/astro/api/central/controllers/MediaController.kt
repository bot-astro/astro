package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import space.astro.api.central.components.middlewares.CanManageGuildMiddleware
import space.astro.api.central.models.requests.MediaCreateBody
import space.astro.api.central.models.responses.MediaResponse
import space.astro.api.central.services.MediaContentType
import space.astro.api.central.services.MediaStorageService
import space.astro.api.central.services.RateLimitingService
import space.astro.shared.core.exceptions.*
import space.astro.shared.core.models.database.MediaEntity
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.properties.R2Properties
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.mongo.MediaRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import kotlin.math.max
import kotlin.time.Duration.Companion.nanoseconds

@Controller
@Tag(name = "Media")
class MediaController(
    private val mediaStorageService: MediaStorageService,
    private val mediaRepository: MediaRepository,
    private val rateLimitingService: RateLimitingService,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val r2Properties: R2Properties,
) {
    private companion object {
        const val MAX_MEDIA_PER_GUILD = 200L
        const val MEDIA_CLEANUP_INTERVAL = 20L
    }

    @Operation(summary = "Get media of the guild")
    @GetMapping(CentralApiEndpoint.GUILD_MEDIA)
    @CanManageGuildMiddleware
    fun getMedia(
        @PathVariable("guildId") guildId: String,
        @RequestParam("type", required = false) type: MediaEntity.MediaType?,
    ): ResponseEntity<List<MediaResponse>> =
        ResponseEntity.ok(
            mediaRepository
                .findAllByGuildId(guildId)
                .filter { type == null || it.type == type }
                .sortedByDescending { it.id.date }
                .map { MediaResponse.from(it, r2Properties) },
        )

    @Operation(summary = "Upload media")
    @PostMapping(CentralApiEndpoint.GUILD_MEDIA)
    @CanManageGuildMiddleware
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
                guildSettingsRepository.findByGuildID(guildId)
                    ?: throw ANotFoundException("Guild with ID $guildId not found in Astro database")

            val usedMediaUrls = getMediaUrlsUsedByGuild(guildSettingsEntity)
            val outdatedMedia = mediaOfGuild.filter { r2Properties.urlOf(it.key) !in usedMediaUrls }

            if (outdatedMedia.isNotEmpty()) {
                mediaStorageService.deleteAll(outdatedMedia.map { it.key })
                mediaRepository.deleteAll(outdatedMedia)
            }
        }

        // save uploaded media
        val mediaKey =
            mediaStorageService.upload(
                type = mediaMeta.mediaType,
                guildId = guildId,
                data = media.bytes,
                contentType =
                    MediaContentType.entries.find { it.mimeType == media.contentType }
                        ?: throw ABadRequestException("Unsupported media type, supported ones are png, jpeg, gif and webp"),
            )

        val mediaEntity =
            MediaEntity(
                guildId = guildId,
                bucket = r2Properties.bucketName,
                key = mediaKey,
                type = mediaMeta.mediaType,
            )

        mediaRepository.save(mediaEntity)

        return ResponseEntity.ok(MediaResponse.from(mediaEntity, r2Properties))
    }

    @Operation(summary = "Delete media")
    @DeleteMapping(CentralApiEndpoint.GUILD_MEDIA_ITEM)
    @CanManageGuildMiddleware
    fun deleteMedia(
        @PathVariable("guildId") guildId: String,
        @PathVariable("mediaId") mediaId: String,
    ): ResponseEntity<Unit> {
        val media =
            mediaRepository.findById(mediaId).orElse(null)?.takeIf { it.guildId == guildId }
                ?: throw ANotFoundException("Media with ID $mediaId not found for guild with id $guildId")

        mediaStorageService.deleteAll(listOf(media.key))
        mediaRepository.delete(media)

        return ResponseEntity.noContent().build()
    }

    private fun getMediaUrlsUsedByGuild(guildSettings: GuildSettingsEntity): Set<String> =
        buildSet {
            addAll(listOfNotNull(guildSettings.avatarUrl, guildSettings.bannerUrl))

            for (i in guildSettings.interfaces) {
                with(i.embedStyle) {
                    addAll(listOfNotNull(thumbnail, image, authorIconUrl, footerIconUrl))
                }
            }
        }
}
