package space.astro.api.central.services.ai

import ai.koog.http.client.KoogHttpClient
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * Sums the cost (USD, OpenRouter credits are dollars) that OpenRouter reports in the `usage` of every response.
 * Put it in the coroutine context of the model calls to collect their cost.
 */
class OpenRouterCostTracker : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<OpenRouterCostTracker>

    @Volatile
    var cost: Double? = null
        private set

    @Synchronized
    fun addFrom(rawResponse: String) {
        // most streaming chunks carry no usage, they are skipped without parsing
        if (!rawResponse.contains("\"cost\"")) {
            return
        }

        val responseCost =
            try {
                Json
                    .parseToJsonElement(rawResponse)
                    .jsonObject["usage"]
                    ?.jsonObject
                    ?.get("cost")
                    ?.jsonPrimitive
                    ?.doubleOrNull
            } catch (_: Exception) {
                null
            } ?: return

        cost = (cost ?: 0.0) + responseCost
    }
}

/**
 * Koog's OpenRouter client drops the cost from the usage it parses, this client reads it from the raw responses
 * and hands it to the [OpenRouterCostTracker] of the calling coroutine
 */
class OpenRouterCostTrackingHttpClient(
    private val delegate: KoogHttpClient,
) : KoogHttpClient by delegate {
    override suspend fun <T : Any, R : Any> post(
        path: String,
        requestBody: T,
        requestBodyType: KClass<T>,
        responseType: KClass<R>,
        parameters: Map<String, String>,
        headers: Map<String, String>,
    ): R {
        val response = delegate.post(path, requestBody, requestBodyType, responseType, parameters, headers)
        if (response is String) {
            currentCoroutineContext()[OpenRouterCostTracker]?.addFrom(response)
        }
        return response
    }

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
        flow {
            val tracker = currentCoroutineContext()[OpenRouterCostTracker]
            delegate
                .sse(
                    path,
                    requestBody,
                    requestBodyType,
                    dataFilter,
                    { data ->
                        tracker?.addFrom(data)
                        decodeStreamingResponse(data)
                    },
                    processStreamingChunk,
                    parameters,
                    headers,
                ).collect { emit(it) }
        }
}
