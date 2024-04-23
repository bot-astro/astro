package space.astro.shared.core.models.redis

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import space.astro.shared.core.components.io.DataSerializer

/**
 * Hashed cache manager
 *
 * Example:
 * ```
 * --> base
 *     |--> {field_1}
 *          |--> data
 *     |--> {field_2}
 *          |--> data
 * --> base
 *     |--> {field_1}
 *          |--> data
 *     |--> {field_2}
 *          |--> data
 * ```
 *
 * @param keyBase the key for all fields
 * @param dataSerializer
 * @param redis [RedisClusterCommands]
 */
class RedisHashCacheDao(
    val keyBase: String,
    val dataSerializer: DataSerializer,
    val redis: RedisClusterCommands<String, String>,
) {
    /**
     * Get a single field from the hash
     */
    inline fun <reified T> get(field: String): T? {
        return redis.hget(keyBase, field)?.let {
            dataSerializer.deserialize(it)
        }
    }

    /**
     * Gets all fields from the hash
     */

    /**
     * Cache all the provided fields in the hash
     *
     * @param fieldToDataMap Map of field name to field data
     */
    inline fun <reified T> cacheAll(fieldToDataMap: Map<String, T>) {
        fieldToDataMap
            .mapValues { mapItem -> dataSerializer.serializeData(mapItem.value) }
            .also {
                redis.hset(keyBase, it)
            }
    }

    /**
     * Cache data in a field of the hash
     *
     * @param field Field for the value
     * @param data Data to cache
     */
    inline fun <reified T> cache(field: String, data: T) {
        dataSerializer.serializeData(data)
            .also {
                redis.hset(keyBase, field, it)
            }
    }

    /**
     * Delete a field from the hash
     *
     * @param field Field to delete
     */
    fun delete(field: String) {
        redis.hdel(keyBase, field)
    }

    /**
     * Delete multiple fields from the hash
     *
     * @param fields Fields to delete
     */
    fun deleteMultiple(vararg fields: String) {
        redis.hdel(keyBase, *fields)
    }
}