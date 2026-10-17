package space.astro.shared.core.autoconfigure

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

@AutoConfiguration
@ConditionalOnClass(RedisClient::class)
@ConditionalOnBean(LettuceConnectionFactory::class)
@AutoConfigureAfter(DataRedisAutoConfiguration::class)
class ARedisAutoConfiguration {
    @Bean
    fun redisClient(connectionFactory: LettuceConnectionFactory): RedisClient = connectionFactory.requiredNativeClient as RedisClient

    @Bean
    fun redisConnection(connectionFactory: LettuceConnectionFactory): StatefulRedisConnection<String, String> {
        val client = connectionFactory.requiredNativeClient as RedisClient
        return client.connect()
    }

    @Bean
    fun redisConnectionByte(connectionFactory: LettuceConnectionFactory): StatefulRedisConnection<String, ByteArray> {
        val client = connectionFactory.requiredNativeClient as RedisClient
        val redisCodec: RedisCodec<String, ByteArray> = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        return client.connect(redisCodec)
    }

    @Bean
    fun redisCommands(connection: StatefulRedisConnection<String, String>): RedisCommands<String, String> = connection.sync()

    @Bean
    fun redisAsyncCommands(connection: StatefulRedisConnection<String, String>): RedisAsyncCommands<String, String> = connection.async()
}
