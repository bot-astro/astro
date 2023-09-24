package space.astro.shared.core.components.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.SingletonSupport
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import space.astro.shared.core.dao.GuildDao
import space.astro.shared.core.models.database.GuildDto

@Component
final class DataSerializer(
    val objectMapper: ObjectMapper
) {

    fun<T> serializeData(clazz: T): String {
        return objectMapper.writeValueAsString(clazz)
    }

    inline fun <reified T> deserialize(serializedData: String): T {
        return objectMapper.readValue(serializedData)
    }

    inline fun <reified T> deserializeList(listOfData: MutableCollection<String>): List<T> {
        return objectMapper.readValue("[${listOfData.joinToString(", ")}]")
    }
}
