package space.astro.shared.core.autoconfigure

import com.mongodb.client.MongoClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@AutoConfiguration
@ConditionalOnClass(MongoClient::class)
@ConditionalOnBean(MongoDatabaseFactory::class)
@AutoConfigureAfter(MongoAutoConfiguration::class, DataMongoAutoConfiguration::class)
@EnableMongoRepositories(basePackages = ["space.astro.shared.core.dao"])
class AMongoDaosAutoConfiguration