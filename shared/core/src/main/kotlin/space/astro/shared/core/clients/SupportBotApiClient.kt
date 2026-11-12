package space.astro.shared.core.clients

import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.properties.support_bot.SupportBotEndpointProperties
import space.astro.shared.core.utils.api.SupportBotApiEndpoint

class SupportBotApiClient(
    supportBotEndpointProperties: SupportBotEndpointProperties,
) {
    private val client =
        RestClient
            .builder()
            .baseUrl(supportBotEndpointProperties.baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, supportBotEndpointProperties.auth)
            .build()

    /**
     * Gives the premium role of the support server to the user
     *
     * @throws ANotFoundException if the user is not in the support server
     */
    fun addPremiumRole(userId: String) {
        try {
            client
                .put()
                .uri(SupportBotApiEndpoint.PREMIUM_ROLE_USER, userId)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("User with id $userId not found in the support server", e)
            }

            throw AUnknownException("Failed to add the premium role to the user", e)
        }
    }

    /**
     * Removes the premium role of the support server from the user
     *
     * @throws ANotFoundException if the user is not in the support server
     */
    fun removePremiumRole(userId: String) {
        try {
            client
                .delete()
                .uri(SupportBotApiEndpoint.PREMIUM_ROLE_USER, userId)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("User with id $userId not found in the support server", e)
            }

            throw AUnknownException("Failed to remove the premium role from the user", e)
        }
    }
}
