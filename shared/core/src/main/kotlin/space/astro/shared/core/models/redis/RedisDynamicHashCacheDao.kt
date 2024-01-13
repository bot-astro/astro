package space.astro.shared.core.models.redis

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import space.astro.shared.core.components.io.DataSerializer

/**
 * Hashed cache manager that accepts a dynamic values as hash keys
 *
 * Allows to have another layer of division of the hash.
 *
 * For example:
 * ```
 * --> base_{dynamic_1}
 *     |--> {field_1}
 *          |--> data
 *     |--> {field_2}
 *          |--> data
 * --> base_{dynamic_2}
 *     |--> {field_1}
 *          |--> data
 *     |--> {field_2}
 *          |--> data
 * ```
 *
 * @param keyBase the base for all the hash keys
 * @param dataSerializer
 * @param redis [RedisClusterCommands]
 */
class RedisDynamicHashCacheDao(
    private val keyBase: String,
    val dataSerializer: DataSerializer,
    val redis: RedisClusterCommands<String, String>,
) {
    /**
     * Constructs the hash key from the base + dynamic
     */
    fun keyName(keyValue: String) = "${keyBase}:$keyValue"

    /**
     * Get all the fields of a specific key of the hash
     */
    inline fun <reified T> getAll(keyValue: String): List<T> {
        return dataSerializer.deserializeList(
            redis.hgetall(keyName(keyValue)).values
        )
    }

    /**
     * Get a single field from the hash
     */
    inline fun <reified T> get(keyValue: String, field: String): T? {
        return redis.hget(keyName(keyValue), field)?.let {
            dataSerializer.deserialize(it)
        }
    }

    /**
     * Cache all the provided fields in the hash
     *
     * @param keyValue Value of the hash key
     * @param fieldToDataMap Map of field name to field data
     */
    inline fun <reified T> cacheAll(keyValue: String, fieldToDataMap: Map<String, T>) {
        fieldToDataMap
            .mapValues { mapItem -> dataSerializer.serializeData(mapItem.value) }
            .also {
                redis.hset(keyName(keyValue), it)
            }
    }

    /**
     * Cache data in a field of the hash
     *
     * @param keyValue Value of the hash key
     * @param field Field for the value
     * @param data Data to cache
     */
    inline fun <reified T> cache(keyValue: String, field: String, data: T) {
        dataSerializer.serializeData(data)
            .also {
                redis.hset(keyName(keyValue), field, it)
            }
    }

    /**
     * Delete a field from the hash
     * @param keyValue Value of the hash key
     * @param field Field to delete
     */
    fun delete(keyValue: String, field: String) {
        redis.hdel(keyName(keyValue), field)
    }

    /**
     * Delete multiple fields from the hash
     * @param keyValue Value of the hash key
     * @param fields Fields to delete
     */
    fun deleteMultiple(keyValue: String, vararg fields: String) {
        redis.hdel(keyName(keyValue), *fields)
    }

    /**
     * Delete all fields from the hash
     * @param keyValue Value of the hash key
     */
    fun deleteAll(keyValue: String) {
        redis.del(keyName(keyValue))
    }
}