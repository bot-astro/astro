package space.astro.shared.core.utils.redis

import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

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
 * @param redis
 * @param jsonMapper
 */
class RedisDynamicHashCacheRepository(
    private val keyBase: String,
    val redis: StringRedisTemplate,
    val jsonMapper: JsonMapper,
) {
    val hashOps: HashOperations<String, String, String> = redis.opsForHash()

    /**
     * Constructs the hash key from the base + dynamic
     */
    fun keyName(keyValue: String) = "${keyBase}:$keyValue"

    /**
     * Get all the fields of a specific key of the hash
     */
    inline fun <reified T> getAll(keyValue: String): List<T> {
        return hashOps.values(keyName(keyValue))
            .map(jsonMapper::readValue)
    }

    inline fun <reified T> get(keyValue: String, field: String): T? {
        return hashOps.get(keyName(keyValue), field)
            ?.let(jsonMapper::readValue)
    }

    inline fun <reified T> cacheAll(keyValue: String, fieldToDataMap: Map<String, T>) {
        fieldToDataMap
            .mapValues { mapItem -> jsonMapper.writeValueAsString(mapItem.value) }
            .also {
                hashOps.putAll(keyName(keyValue), it)
            }
    }

    inline fun <reified T> cache(keyValue: String, field: String, data: T) {
        hashOps.put(keyName(keyValue), field, jsonMapper.writeValueAsString(data))
    }

    fun delete(keyValue: String, field: String) {
        hashOps.delete(keyName(keyValue), field)
    }

    fun deleteMultiple(keyValue: String, vararg fields: String) {
        hashOps.delete(keyName(keyValue), *fields)
    }

    fun deleteAll(keyValue: String) {
        hashOps.delete(keyName(keyValue))
    }
}