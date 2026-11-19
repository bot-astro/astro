package space.astro.api.central.services.ai

import ai.koog.http.client.KoogHttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class OpenRouterCostTrackerTest {
    private val fakeClient =
        object : KoogHttpClient {
            override val clientName = "fake"

            override suspend fun <R : Any> get(
                path: String,
                responseType: KClass<R>,
                parameters: Map<String, String>,
                headers: Map<String, String>,
            ): R = error("not used")

            @Suppress("UNCHECKED_CAST")
            override suspend fun <T : Any, R : Any> post(
                path: String,
                requestBody: T,
                requestBodyType: KClass<T>,
                responseType: KClass<R>,
                parameters: Map<String, String>,
                headers: Map<String, String>,
            ): R = """{"choices":[],"usage":{"prompt_tokens":10,"completion_tokens":2,"cost":0.0015}}""" as R

            override fun <T : Any, R : Any, O : Any> sse(
                path: String,
                requestBody: T,
                requestBodyType: KClass<T>,
                dataFilter: (String?) -> Boolean,
                decodeStreamingResponse: (String) -> R,
                processStreamingChunk: (R) -> O?,
                parameters: Map<String, String>,
                headers: Map<String, String>,
            ): Flow<O> =
                flowOf(
                    """{"choices":[{"delta":{"content":"hi"}}]}""",
                    """{"choices":[],"usage":{"prompt_tokens":10,"completion_tokens":2,"cost":0.002}}""",
                ).mapNotNull { processStreamingChunk(decodeStreamingResponse(it)) }

            override fun <T : Any> lines(
                path: String,
                requestBody: T,
                requestBodyType: KClass<T>,
                parameters: Map<String, String>,
                headers: Map<String, String>,
            ): Flow<String> = error("not used")

            override fun close() = Unit
        }

    private val client = OpenRouterCostTrackingHttpClient(fakeClient)

    @Test
    fun `sums the cost of streamed and plain responses of the calling coroutine`() {
        val tracker = OpenRouterCostTracker()

        runBlocking(tracker) {
            client.sse("p", "body", String::class, { true }, { it }, { it }).toList()
            client.post("p", "body", String::class, String::class)
        }

        assertEquals(0.0035, tracker.cost!!, 1e-9)
    }

    @Test
    fun `the cost stays null when no response reports it`() {
        val tracker = OpenRouterCostTracker()
        tracker.addFrom("""{"choices":[],"usage":{"prompt_tokens":10}}""")
        tracker.addFrom("not json \"cost\"")

        assertNull(tracker.cost)
    }
}
