/*
 * Copyright (c) 2023. Hydra Bot
 */

package space.astro.shared.core.components.kmongo

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "io.kmongo")
data class KmongoConfig(

    var connectionString: String = "",
    var database: String = ""
)
