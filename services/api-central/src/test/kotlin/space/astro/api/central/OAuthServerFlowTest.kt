package space.astro.api.central

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.util.UriComponentsBuilder
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mongodb.MongoDBContainer
import space.astro.api.central.models.mcp.McpClientMetadata
import space.astro.api.central.services.McpClientMetadataService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.models.discord.DiscordTokenDto
import space.astro.shared.core.models.discord.DiscordUserDto
import space.astro.shared.core.properties.DiscordOAuthProperties
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private const val CLIENT_ID = "https://client.example/oauth/metadata.json"
private const val REDIRECT_URI = "https://client.example/callback"
private const val CODE_VERIFIER = "verifier-verifier-verifier-verifier-verifier-verifier"

/**
 * OAuth flow of the MCP clients, from the authorization request to the grant management, against real Mongo and Redis.
 * Discord and the fetch of the client metadata document are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OAuthServerFlowTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mongo = MongoDBContainer("mongo:8")

        @Container
        @ServiceConnection(name = "redis")
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine").withExposedPorts(6379)

        private val nextUserId = AtomicLong(100_000_000_000_000_000)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Autowired
    private lateinit var discordOAuthProperties: DiscordOAuthProperties

    @MockitoBean
    private lateinit var discordApiClient: DiscordApiClient

    @MockitoBean
    private lateinit var mcpClientMetadataService: McpClientMetadataService

    private lateinit var userId: String

    private data class Tokens(
        val accessToken: String,
        val refreshToken: String,
        val scope: String,
    )

    @BeforeEach
    fun setUp() {
        userId = nextUserId.incrementAndGet().toString()
        given(mcpClientMetadataService.fetch(CLIENT_ID)).willReturn(McpClientMetadata(CLIENT_ID, "Test Client", listOf(REDIRECT_URI)))
        given(discordApiClient.getAccessToken(anyString(), eq(discordOAuthProperties) ?: discordOAuthProperties))
            .willReturn(DiscordTokenDto("discord-access-$userId", 604800, "discord-refresh-$userId", "identify", "Bearer", null))
        given(discordApiClient.getSelfUser(anyString())).willReturn(DiscordUserDto(userId, "user", "0", null, "user@example.com"))
    }

    @Test
    fun `authorization, token, refresh and introspection`() {
        val metadata = json(mockMvc.perform(get("/.well-known/oauth-authorization-server")).andReturn())
        assertEquals("http://localhost/oauth/revoke", metadata["revocation_endpoint"].asString())

        val (code, _) = authorize("guilds:write")
        val tokens = exchangeCode(code)
        assertEquals("guilds:read guilds:write", tokens.scope)

        val introspection = introspect(tokens.accessToken)
        assertTrue(introspection["active"].asBoolean())
        assertEquals(userId, introspection["sub"].asString())
        assertEquals(CLIENT_ID, introspection["client_id"].asString())
        assertEquals("http://localhost:9002/mcp", introspection["aud"].asString())

        val me = mockMvc.perform(get("/v2/auth/me").bearer(tokens.accessToken)).andReturn()
        assertEquals(200, me.response.status)
        assertEquals(userId, json(me)["id"].asString())

        val refreshed = refresh(tokens.refreshToken)
        assertNotNull(refreshed)
        assertNotEquals(tokens.accessToken, refreshed!!.accessToken)
        assertTrue(introspect(refreshed.accessToken)["active"].asBoolean())
    }

    @Test
    fun `consent can reduce the scopes and can be denied`() {
        val (code, _) = authorize("guilds:write", grantedScope = "guilds:read")
        val tokens = exchangeCode(code)
        assertEquals("guilds:read", tokens.scope)

        val denied = authorizeRedirect("guilds:read", decision = "deny")
        assertEquals("access_denied", denied.queryParams.getFirst("error"))
    }

    @Test
    fun `reusing a refresh token revokes the grant`() {
        val tokens = exchangeCode(authorize("guilds:read").first)
        val refreshed = refresh(tokens.refreshToken)!!

        assertNull(refresh(tokens.refreshToken))
        assertTrue(!introspect(refreshed.accessToken)["active"].asBoolean())
        assertNull(refresh(refreshed.refreshToken))
    }

    @Test
    fun `reusing an authorization code revokes the tokens issued from it`() {
        val code = authorize("guilds:read").first
        val tokens = exchangeCode(code)

        val second =
            tokenRequest(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to REDIRECT_URI,
                "code_verifier" to CODE_VERIFIER,
            )
        assertEquals(400, second.response.status)
        assertEquals("invalid_grant", json(second)["error"].asString())

        assertTrue(!introspect(tokens.accessToken)["active"].asBoolean())
        assertNull(refresh(tokens.refreshToken))
    }

    @Test
    fun `revoking a refresh token revokes the grant, unknown tokens are accepted`() {
        val tokens = exchangeCode(authorize("guilds:read").first)

        assertEquals(200, revoke(tokens.refreshToken).response.status)
        assertTrue(!introspect(tokens.accessToken)["active"].asBoolean())
        assertNull(refresh(tokens.refreshToken))

        assertEquals(200, revoke("unknown").response.status)
    }

    @Test
    fun `revoking an access token keeps the refresh token`() {
        val tokens = exchangeCode(authorize("guilds:read").first)

        revoke(tokens.accessToken)
        assertTrue(!introspect(tokens.accessToken)["active"].asBoolean())
        assertNotNull(refresh(tokens.refreshToken))
    }

    @Test
    fun `grants are listed and deleted with the dashboard session`() {
        val (code, session) = authorize("guilds:write")
        val tokens = exchangeCode(code)

        val beforeUse = json(mockMvc.perform(get("/v2/auth/grants").cookie(session)).andReturn())
        assertEquals(1, beforeUse.size())
        val grant = beforeUse[0]
        assertEquals(CLIENT_ID, grant["client_id"].asString())
        assertEquals("Test Client", grant["client_name"].asString())
        assertEquals("""["guilds:read","guilds:write"]""", grant["scopes"].toString())
        assertNotNull(grant["created_at"].asString())
        assertTrue(grant["last_used_at"].isNull)

        introspect(tokens.accessToken)
        val afterUse = json(mockMvc.perform(get("/v2/auth/grants").cookie(session)).andReturn())
        assertTrue(!afterUse[0]["last_used_at"].isNull)

        val listWithBearer = mockMvc.perform(get("/v2/auth/grants").bearer(tokens.accessToken)).andReturn()
        assertEquals(403, listWithBearer.response.status)

        val unknown = mockMvc.perform(delete("/v2/auth/grants/unknown").cookie(session)).andReturn()
        assertEquals(404, unknown.response.status)

        val deleted = mockMvc.perform(delete("/v2/auth/grants/${grant["id"].asString()}").cookie(session)).andReturn()
        assertEquals(204, deleted.response.status)
        assertTrue(!introspect(tokens.accessToken)["active"].asBoolean())
        assertNull(refresh(tokens.refreshToken))
        assertEquals(0, json(mockMvc.perform(get("/v2/auth/grants").cookie(session)).andReturn()).size())
    }

    /**
     * Goes through authorize, the Discord login and the consent
     *
     * @return the authorization code and the dashboard session cookie created by the login
     */
    private fun authorize(
        scope: String,
        grantedScope: String = scope,
    ): Pair<String, Cookie> {
        lateinit var session: Cookie
        val redirect = authorizeRedirect(scope, grantedScope = grantedScope) { session = it }
        assertEquals("state-1", redirect.queryParams.getFirst("state"))
        assertEquals("http://localhost", redirect.queryParams.getFirst("iss"))
        return Pair(redirect.queryParams.getFirst("code")!!, session)
    }

    private fun authorizeRedirect(
        scope: String,
        decision: String = "approve",
        grantedScope: String = scope,
        onSession: (Cookie) -> Unit = {},
    ): org.springframework.web.util.UriComponents {
        val challenge =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(CODE_VERIFIER.toByteArray()),
            )
        val authorize =
            mockMvc
                .perform(
                    get("/oauth/authorize")
                        .param("client_id", CLIENT_ID)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("response_type", "code")
                        .param("code_challenge", challenge)
                        .param("code_challenge_method", "S256")
                        .param("state", "state-1")
                        .param("scope", scope),
                ).andReturn()
        assertEquals(302, authorize.response.status)

        val login = mockMvc.perform(get(authorize.response.redirectedUrl!!)).andReturn()
        val discordState =
            UriComponentsBuilder
                .fromUriString(login.response.redirectedUrl!!)
                .build()
                .queryParams
                .getFirst("state")

        val callback =
            mockMvc
                .perform(get("/v2/auth/discord/callback").param("code", "discord-code").param("state", discordState!!))
                .andReturn()
        val consentUrl = callback.response.redirectedUrl!!
        assertTrue(consentUrl.startsWith("/oauth/consent"), consentUrl)
        onSession(callback.response.getCookie("astro_session")!!)

        val consentPage = mockMvc.perform(get(consentUrl)).andReturn()
        assertEquals(200, consentPage.response.status)
        assertTrue(consentPage.response.contentAsString.contains("Test Client"))

        val requestId =
            UriComponentsBuilder
                .fromUriString(consentUrl)
                .build()
                .queryParams
                .getFirst("request_id")
        val consent =
            mockMvc
                .perform(
                    post("/oauth/consent")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("request_id", requestId!!)
                        .param("decision", decision)
                        .param("scope", grantedScope),
                ).andReturn()
        assertEquals(302, consent.response.status)

        val redirect = UriComponentsBuilder.fromUriString(consent.response.redirectedUrl!!).build()
        assertEquals(REDIRECT_URI, "${redirect.scheme}://${redirect.host}${redirect.path}")
        return redirect
    }

    private fun exchangeCode(code: String): Tokens {
        val result =
            tokenRequest(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to REDIRECT_URI,
                "code_verifier" to CODE_VERIFIER,
            )
        assertEquals(200, result.response.status, result.response.contentAsString)
        return tokens(json(result))
    }

    private fun refresh(refreshToken: String): Tokens? {
        val result = tokenRequest("grant_type" to "refresh_token", "refresh_token" to refreshToken)
        if (result.response.status != 200) {
            assertEquals("invalid_grant", json(result)["error"].asString())
            return null
        }
        return tokens(json(result))
    }

    private fun tokenRequest(vararg params: Pair<String, String>): MvcResult =
        mockMvc
            .perform(
                post("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("client_id", CLIENT_ID)
                    .apply { params.forEach { (key, value) -> param(key, value) } },
            ).andReturn()

    private fun introspect(token: String): JsonNode =
        json(
            mockMvc
                .perform(post("/oauth/introspect").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("token", token))
                .andReturn(),
        )

    private fun revoke(token: String): MvcResult =
        mockMvc
            .perform(
                post("/oauth/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("token", token)
                    .param("client_id", CLIENT_ID),
            ).andReturn()

    private fun tokens(body: JsonNode) = Tokens(body["access_token"].asString(), body["refresh_token"].asString(), body["scope"].asString())

    private fun json(result: MvcResult): JsonNode = jsonMapper.readTree(result.response.contentAsString)

    private fun MockHttpServletRequestBuilder.bearer(token: String) = header(HttpHeaders.AUTHORIZATION, "Bearer $token")
}
