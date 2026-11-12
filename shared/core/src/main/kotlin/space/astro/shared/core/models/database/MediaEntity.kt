package space.astro.shared.core.models.database

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "media")
data class MediaEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed
    val guildId: String,
    val type: MediaType,
    val bucket: String,
    val key: String,
    // original name of the file uploaded by the user
    val fileName: String? = null,
) {
    enum class MediaType {
        PROFILE_AVATAR,
        PROFILE_BANNER,
        INTERFACE_IMAGE,
        ;

        /**
         * @return the storage path for s3 like storages
         */
        fun storagePath(guildId: String): String =
            when (this) {
                PROFILE_BANNER -> "guilds/$guildId/banner/"
                PROFILE_AVATAR -> "guilds/$guildId/avatar/"
                INTERFACE_IMAGE -> "guilds/$guildId/interface/"
            }
    }
}
