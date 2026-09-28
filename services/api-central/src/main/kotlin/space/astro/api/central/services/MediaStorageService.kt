package space.astro.api.central.services

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import space.astro.shared.core.models.database.MediaEntity
import space.astro.shared.core.properties.R2Properties

@Service
class MediaStorageService(
    private val r2Properties: R2Properties
) {
    private val s3: S3Client = runBlocking {
        S3Client.fromEnvironment {
            region = "auto"
            endpointUrl = Url.parse(r2Properties.endpointUrl)
            credentialsProvider = StaticCredentialsProvider(
                Credentials(
                    accessKeyId = r2Properties.accessKeyId,
                    secretAccessKey = r2Properties.secretAccessKey
                )
            )
        }
    }

    /**
     * Uploads a media to Cloudflare R2.
     *
     * @return the URL of the uploaded media
     */
    fun upload(
        type: MediaEntity.MediaType,
        guildId: String,
        data: ByteArray,
        contentType: MediaContentType,
    ): String {
        val key = type.storagePath(guildId) + NanoIdUtils.randomNanoId() + "." + contentType.extension

        runBlocking {
            s3.putObject {
                this.bucket = r2Properties.bucketName
                this.key = key
                this.contentType = contentType.mimeType
                this.body = ByteStream.fromBytes(data)
            }
        }

        return key
    }
}

enum class MediaContentType(val mimeType: String, val extension: String) {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    companion object {

        /**
         * @throws IllegalArgumentException if mime type is not supported
         */
        fun fromMime(mimeType: String): MediaContentType {
            return entries.find { it.mimeType == mimeType }
                ?: throw IllegalArgumentException("Unknown mime type: $mimeType")
        }
    }
}