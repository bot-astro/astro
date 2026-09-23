package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("r2")
data class R2Properties(
    val endpointUrl: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String
)