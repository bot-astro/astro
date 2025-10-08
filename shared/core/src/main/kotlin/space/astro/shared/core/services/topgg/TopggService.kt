package space.astro.shared.core.services.topgg

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import space.astro.shared.core.configs.TopggConfig
import space.astro.shared.core.models.topgg.TopggVoteResponse
import java.net.URI
import java.time.Instant

private val log = KotlinLogging.logger { }

@Service
class TopggService(
    private val topggConfig: TopggConfig,
) {
    private val httpClient = HttpClient(Apache) {
        expectSuccess = true
        install(Logging)
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        // for a better UX, we do not retry 5xx responses
//        install(HttpRequestRetry) {
//            retryOnServerErrors(maxRetries = 3)
//            exponentialDelay()
//        }
        install(HttpTimeout)
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(topggConfig.token)
        }
    }

    /**
     * Checks whether a user has voted on top.gg in the last 12 hours
     *
     * @param userId Optional user id filter
     * @param timeoutMillis Optional timeout for the request, default is 2000 ms to avoid exceeding Discord 3 seconds initial interaction reply limit
     */
    suspend fun lastUserVote(
        userId: String,
        timeoutMillis: Long? = 2000
    ): Long? {
        try {
            val uri = UriComponentsBuilder.fromUri(URI("https://top.gg/api/v1"))
                .pathSegment("projects", "@me", "votes", userId)
                .queryParam("source", "discord")
                .build()
                .toUriString()

            val body: TopggVoteResponse = httpClient.get(uri) {
                timeout {
                    requestTimeoutMillis = timeoutMillis
                }
            }.body()

            return body.createdAt.toEpochMilli()
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404)
                return null
            else
                log.error("Failed fetching last vote from top.gg", e)
                return System.currentTimeMillis()
        } catch (t: Throwable) {
            log.error("Failed fetching last vote from top.gg", t)
            return System.currentTimeMillis()
        }
    }
}