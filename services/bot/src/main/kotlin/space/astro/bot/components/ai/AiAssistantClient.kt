package space.astro.bot.components.ai

import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import space.astro.bot.properties.AiAssistantProperties
import java.net.http.HttpClient
import java.time.Duration

/**
 * Internal endpoints of api-central for the AI assistant, they all run as the Discord user given in the request
 *
 * Every call blocks, run them on the IO dispatcher
 */
@Component
class AiAssistantClient(
    properties: AiAssistantProperties,
) {
    private companion object {
        const val INTERNAL_TOKEN_HEADER = "X-Internal-Token"
        const val TURN = "/internal/ai/guilds/{guildId}/turn"
        const val PROPOSAL_APPLY = "/internal/ai/guilds/{guildId}/proposals/{proposalId}/apply"
        const val PROPOSAL_CANCEL = "/internal/ai/guilds/{guildId}/proposals/{proposalId}/cancel"
    }

    private val client =
        RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader(INTERNAL_TOKEN_HEADER, properties.internalToken)
            .requestFactory(
                JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()).apply {
                    setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds))
                },
            ).build()

    /**
     * @throws org.springframework.web.client.HttpClientErrorException.Forbidden if the user cannot manage the guild
     */
    fun turn(
        guildId: String,
        request: AiTurnRequest,
    ): AiTurnResponse =
        client
            .post()
            .uri(TURN, guildId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body<AiTurnResponse>() ?: AiTurnResponse()

    fun apply(
        guildId: String,
        proposalId: String,
        userId: String,
    ): AiApplyOutcome =
        try {
            client
                .post()
                .uri(PROPOSAL_APPLY, guildId, proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(AiUserRequest(userId))
                .retrieve()
                .toBodilessEntity()

            AiApplyOutcome.APPLIED
        } catch (e: RestClientResponseException) {
            when (e.statusCode.value()) {
                403 -> AiApplyOutcome.FORBIDDEN
                404, 409 -> AiApplyOutcome.CONFLICT
                else -> throw e
            }
        }

    fun cancel(
        guildId: String,
        proposalId: String,
        userId: String,
    ) {
        try {
            client
                .post()
                .uri(PROPOSAL_CANCEL, guildId, proposalId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(AiUserRequest(userId))
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientResponseException) {
            // already expired or cancelled, the outcome the user wanted
            if (e.statusCode.value() != 404) {
                throw e
            }
        }
    }
}
