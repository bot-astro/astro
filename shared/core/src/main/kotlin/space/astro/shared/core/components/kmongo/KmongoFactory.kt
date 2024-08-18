/*
 * Copyright (c) 2023. Hydra Bot
 */

package space.astro.shared.core.components.kmongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.KMongo
import org.litote.kmongo.util.ObjectMappingConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import space.astro.shared.core.configs.KmongoConfig

@Component
class KmongoFactory {

    @Bean
    fun getMongoClient(kmongoConfig: KmongoConfig): MongoClient {
        ObjectMappingConfiguration.serializeNull = false
        return KMongo.createClient(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(kmongoConfig.connectionString))
                .applyToConnectionPoolSettings { builder ->
                    builder.maxSize(20)
                }
                .build()
        )
    }

    @Bean
    fun getDatabase(mongoClient: MongoClient, kmongoConfig: KmongoConfig): MongoDatabase {
        return mongoClient.getDatabase(kmongoConfig.database)
    }
}
