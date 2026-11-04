package space.astro.api.central.models.responses

import space.astro.shared.core.models.database.MediaEntity
import space.astro.shared.core.properties.R2Properties

data class MediaResponse(
    val id: String,
    val type: MediaEntity.MediaType,
    val fileName: String,
    val url: String,
    val createdAt: Long,
) {
    companion object {
        fun from(
            entity: MediaEntity,
            r2Properties: R2Properties,
        ) = MediaResponse(
            id = entity.id.toHexString(),
            type = entity.type,
            fileName = entity.fileName ?: entity.key.substringAfterLast("/"),
            url = r2Properties.urlOf(entity.key),
            createdAt = entity.id.date.time,
        )
    }
}
