package space.astro.shared.core.models.redis

import io.lettuce.core.SetArgs
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import space.astro.shared.core.components.io.DataSerializer

/**
 * Expiring cache manager
 *
 * Example:
 * ```
 * --> {key_base}:{key_1}
 *     |--> data
 * --> {key_base}:{key_2}
 *     |--> data
 * ```
 *
 * @param keyBase the key base for all keys
 * @param dataSerializer
 * @param redis [RedisClusterCommands]
 */
class RedisExpiringCacheDao(
    val keyBase: String,
    val dataSerializer: DataSerializer,
    val redis: RedisClusterCommands<String, String>,
) {
    /**
     * Constructs the key from the base + dynamic
     */
    fun keyName(keyValue: String) = "${keyBase}:$keyValue"

    /**
     * Get a single value
     */
    inline fun <reified T> get(keyValue: String): T? {
        return redis.get(keyName(keyValue))?.let {
            dataSerializer.deserialize(it)
        }
    }

    /**
     * Cache a single value that expires
     *
     * @param keyValue Value of the key
     * @param data Data to cache
     * @param ex Expiration in seconds
     */
    inline fun <reified T> cache(keyValue: String, data: T, ex: Long) {
        dataSerializer.serializeData(data)
            .also {
                redis.set(keyName(keyValue), it, SetArgs.Builder.ex(ex))
            }
    }

    /**
     * Delete a key
     * @param keyValue Value of the hash key
     */
    fun delete(keyValue: String) {
        redis.del(keyName(keyValue))
    }
}