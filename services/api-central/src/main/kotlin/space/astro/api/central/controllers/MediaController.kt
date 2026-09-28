package space.astro.api.central.controllers

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import space.astro.api.central.models.requests.MediaCreateBody
import space.astro.api.central.services.MediaContentType
import space.astro.api.central.services.MediaStorageService
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.models.database.MediaEntity
import space.astro.shared.core.repositories.MediaRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint

@Controller
class MediaController(
    private val mediaStorageService: MediaStorageService,
    private val mediaRepository: MediaRepository
) {
    @PostMapping(CentralApiEndpoint.GUILD_MEDIA)
    fun uploadMedia(
        @PathVariable("guildId") guildId: String,
        @RequestPart("meta", required = true) mediaMeta: MediaCreateBody,
        @RequestPart("media", required = true) media: MultipartFile
    ): ResponseEntity<MediaEntity> {
        val mediaUri = mediaStorageService.upload(
            type = mediaMeta.mediaType,
            guildId = guildId,
            data = media.bytes,
            contentType = MediaContentType.fromMime(
                media.originalFilename?.substringAfterLast(".")
                    ?: throw ABadRequestException("Missing file name for media")
            )
        )

        val mediaEntity = MediaEntity(
            guildId = guildId,
            uri = mediaUri,
            type = mediaMeta.mediaType,
        )

        mediaRepository.save(mediaEntity)

        return ResponseEntity.ok(mediaEntity)
    }
}