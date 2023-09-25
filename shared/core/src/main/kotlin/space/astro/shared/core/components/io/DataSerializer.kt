package space.astro.shared.core.components.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component

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
