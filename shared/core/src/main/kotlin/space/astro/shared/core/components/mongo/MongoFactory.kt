package space.astro.shared.core.components.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import space.astro.shared.core.models.database.*

/**
 * Factory for [MongoClient] and [MongoDatabase]
 */
@Component
class MongoFactory {

    @Bean
    fun getMongoClient(mongoConfig: MongoConfig): MongoClient {
        return MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(
                    ConnectionString(mongoConfig.connectionString)
                )
                .applyToConnectionPoolSettings { builder ->
                    builder
                        .maxSize(20)
                }
                .codecRegistry(
                    CodecRegistries.fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                .automatic(true)
                                .register(
                                    // USER
                                    UserData::class.java,
                                    UserEntitlement::class.java,
                                    UserSettingsDto::class.java,
                                    GuildUpgradeData::class.java,
                                    // GUILD
                                    GuildData::class.java,
                                    GuildEntitlement::class.java,
                                    TemplateData::class.java,
                                    ConnectionData::class.java, // Action?
                                    GeneratorData::class.java,
                                    RenameConditions::class.java,
                                    CommandsSettings::class.java,
                                    InterfaceData::class.java,
                                    InterfaceButton::class.java,
                                    InterfaceAction::class.java,
                                    EmbedStyle::class.java
                                )
                                .build()
                        )
                    )
                )
                .build()
        )
    }

    @Bean
    fun getMongoDatabase(client: MongoClient, config: MongoConfig): MongoDatabase {
        return client.getDatabase(config.database)
    }
}