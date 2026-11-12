package space.astro.api.central.models.requests

import space.astro.shared.core.models.database.MediaEntity

data class MediaCreateBody(
    val mediaType: MediaEntity.MediaType,
)
