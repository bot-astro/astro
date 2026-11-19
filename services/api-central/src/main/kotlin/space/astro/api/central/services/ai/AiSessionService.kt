package space.astro.api.central.services.ai

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Conversation history of the assistant, gzipped in Redis
 */
@Service
class AiSessionService(
    private val redis: StringRedisTemplate,
) {
    private companion object {
        val SESSION_TTL: Duration = Duration.ofHours(24)
        const val MAX_MESSAGES = 40

        val json = Json { ignoreUnknownKeys = true }
        val serializer = ListSerializer(Message.serializer())
    }

    fun load(sessionKey: String): List<Message> {
        val stored = redis.opsForValue().get(sessionKey) ?: return emptyList()
        val decoded = GZIPInputStream(Base64.getDecoder().decode(stored).inputStream()).use { it.readBytes() }
        return try {
            json.decodeFromString(serializer, decoded.decodeToString())
        } catch (_: Exception) {
            // a history written by an older version of the messages is dropped instead of failing every turn
            emptyList()
        }
    }

    fun save(
        sessionKey: String,
        messages: List<Message>,
    ) {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { it.write(json.encodeToString(serializer, trim(messages)).encodeToByteArray()) }
        redis.opsForValue().set(sessionKey, Base64.getEncoder().encodeToString(bytes.toByteArray()), SESSION_TTL)
    }

    /**
     * Keeps the last messages, starting from a message written by the user
     * so that a tool result is never kept without the call that produced it
     */
    fun trim(messages: List<Message>): List<Message> {
        if (messages.size <= MAX_MESSAGES) {
            return messages
        }

        val start =
            (messages.size - MAX_MESSAGES until messages.size).firstOrNull { index ->
                val message = messages[index]
                message is Message.User && message.parts.none { it is MessagePart.Tool.Result }
            } ?: return emptyList()

        return messages.subList(start, messages.size)
    }
}
