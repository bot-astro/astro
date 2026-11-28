package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.HtmlUtils
import org.springframework.web.util.UriComponentsBuilder
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.auth.AuthScope
import space.astro.api.central.models.mcp.McpAuthRequest
import space.astro.api.central.services.InvalidClientMetadataException
import space.astro.api.central.services.McpClientMetadataService
import space.astro.api.central.services.McpOAuthService
import space.astro.api.central.services.McpTokenPair
import space.astro.shared.core.properties.api_central.CentralApiProperties
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.net.URI
import java.time.Instant

private const val MIN_CODE_VERIFIER_LENGTH = 43
private const val MAX_CODE_VERIFIER_LENGTH = 128

/**
 * OAuth authorization server for MCP clients, whose `client_id` is the URL of a Client ID Metadata Document.
 * Users log in with the same Discord OAuth flow of the dashboard, then they have to give consent to the client.
 *
 * Hidden from the OpenAPI spec because these are not endpoints to expose as tools.
 */
@Hidden
@RestController
class OAuthServerController(
    centralApiProperties: CentralApiProperties,
    private val mcpClientMetadataService: McpClientMetadataService,
    private val mcpOAuthService: McpOAuthService,
) {
    private val issuer = centralApiProperties.publicUrl.trimEnd('/')
    private val mcpResource = centralApiProperties.mcpResourceUrl.trimEnd('/')

    @GetMapping(CentralApiEndpoint.OAUTH_SERVER_METADATA)
    fun metadata(): Map<String, Any> =
        mapOf(
            "issuer" to issuer,
            "authorization_endpoint" to issuer + CentralApiEndpoint.OAUTH_AUTHORIZE,
            "token_endpoint" to issuer + CentralApiEndpoint.OAUTH_TOKEN,
            "introspection_endpoint" to issuer + CentralApiEndpoint.OAUTH_INTROSPECT,
            "revocation_endpoint" to issuer + CentralApiEndpoint.OAUTH_REVOKE,
            "revocation_endpoint_auth_methods_supported" to listOf("none"),
            "response_types_supported" to listOf("code"),
            "grant_types_supported" to listOf("authorization_code", "refresh_token"),
            "code_challenge_methods_supported" to listOf("S256"),
            "token_endpoint_auth_methods_supported" to listOf("none"),
            "scopes_supported" to AuthScope.ALL,
            "client_id_metadata_document_supported" to true,
            "authorization_response_iss_parameter_supported" to true,
        )

    @GetMapping(CentralApiEndpoint.OAUTH_AUTHORIZE)
    fun authorize(
        @RequestParam("client_id", required = false) clientId: String?,
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
        @RequestParam("response_type", required = false) responseType: String?,
        @RequestParam("code_challenge", required = false) codeChallenge: String?,
        @RequestParam("code_challenge_method", required = false) codeChallengeMethod: String?,
        @RequestParam("state", required = false) state: String?,
        @RequestParam("scope", required = false) scope: String?,
        @RequestParam("resource", required = false) resource: String?,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal?,
    ): ResponseEntity<String> {
        if (clientId == null || redirectUri == null) {
            return errorPage("Missing client_id or redirect_uri.")
        }

        val client =
            try {
                mcpClientMetadataService.fetch(clientId)
            } catch (e: InvalidClientMetadataException) {
                return errorPage("Invalid client: ${e.message}")
            }

        if (redirectUri !in client.redirectUris) {
            return errorPage("The redirect_uri is not registered by the client.")
        }

        // from here the redirect URI is trusted, so the errors go back to the client
        if (responseType != "code") {
            return redirectToClient(redirectUri, state, "error" to "unsupported_response_type")
        }
        if (codeChallenge.isNullOrEmpty() || codeChallengeMethod != "S256") {
            return redirectToClient(redirectUri, state, "error" to "invalid_request", "error_description" to "PKCE with S256 is required")
        }

        val scopes =
            AuthScope.parse(scope)
                ?: return redirectToClient(
                    redirectUri,
                    state,
                    "error" to "invalid_scope",
                    "error_description" to "Supported scopes: ${AuthScope.ALL.joinToString(" ")}",
                )

        if (resource != null && resource.trimEnd('/') != mcpResource) {
            return redirectToClient(redirectUri, state, "error" to "invalid_target", "error_description" to "Unknown resource")
        }

        val requestId =
            mcpOAuthService.createAuthRequest(
                McpAuthRequest(
                    clientId = client.clientId,
                    clientName = client.clientName,
                    redirectUri = redirectUri,
                    codeChallenge = codeChallenge,
                    state = state,
                    scopes = scopes,
                    resource = mcpResource,
                    userId = authPrincipal?.userId,
                ),
            )

        val next =
            if (authPrincipal != null) {
                UriComponentsBuilder
                    .fromPath(CentralApiEndpoint.OAUTH_CONSENT)
                    .queryParam("request_id", requestId)
            } else {
                UriComponentsBuilder
                    .fromPath(CentralApiEndpoint.DISCORD_LOGIN)
                    .queryParam("redirect_path", "/")
                    .queryParam("mcp_request_id", requestId)
            }

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(next.build().toUri())
            .build()
    }

    @GetMapping(CentralApiEndpoint.OAUTH_CONSENT, produces = [MediaType.TEXT_HTML_VALUE])
    fun consentPage(
        @RequestParam("request_id") requestId: String,
    ): ResponseEntity<String> {
        val request = mcpOAuthService.getAuthRequest(requestId)
        if (request?.userId == null) {
            return errorPage("This authorization request is not valid or has expired, start again from your MCP client.")
        }

        val clientName = HtmlUtils.htmlEscape(request.clientName)
        val clientHost = HtmlUtils.htmlEscape(URI(request.clientId).host)
        val escapedRequestId = HtmlUtils.htmlEscape(requestId)
        val scopeChoice =
            if (AuthScope.GUILDS_WRITE in request.scopes) {
                """
                <p>It asks to read and change the settings of the servers you manage, as you can in the dashboard.</p>
                <p>
                  <label><input type="radio" name="scope" value="${AuthScope.GUILDS_WRITE}" checked> Read and change</label><br>
                  <label><input type="radio" name="scope" value="${AuthScope.GUILDS_READ}"> Read only</label>
                </p>
                """.trimIndent()
            } else {
                "<p>It will be able to read the settings of the servers you manage, but not change them.</p>"
            }

        return htmlResponse(
            HttpStatus.OK,
            """
            <h1>Authorize $clientName?</h1>
            <p><strong>$clientName</strong> (<code>$clientHost</code>) wants to access your Astro account.</p>
            <p>Only continue if you started this from an app you trust.</p>
            <form method="post" action="${CentralApiEndpoint.OAUTH_CONSENT}">
              <input type="hidden" name="request_id" value="$escapedRequestId">
              $scopeChoice
              <button type="submit" name="decision" value="approve">Allow</button>
              <button type="submit" name="decision" value="deny">Deny</button>
            </form>
            """.trimIndent(),
        )
    }

    @PostMapping(CentralApiEndpoint.OAUTH_CONSENT, consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun consent(
        @RequestParam("request_id") requestId: String,
        @RequestParam("decision") decision: String,
        @RequestParam("scope", required = false) scope: String?,
    ): ResponseEntity<String> {
        if (decision != "approve") {
            val request =
                mcpOAuthService.denyAuthRequest(requestId)
                    ?: return errorPage("This authorization request is not valid or has expired, start again from your MCP client.")
            return redirectToClient(request.redirectUri, request.state, "error" to "access_denied")
        }

        val request =
            mcpOAuthService.getAuthRequest(requestId)
                ?: return errorPage("This authorization request is not valid or has expired, start again from your MCP client.")

        // the user can grant less than what the client asked for, never more
        val chosenScopes = AuthScope.parse(scope)
        val grantedScopes = if (chosenScopes != null && request.scopes.containsAll(chosenScopes)) chosenScopes else request.scopes

        val (_, code) =
            mcpOAuthService.approveAuthRequest(requestId, grantedScopes)
                ?: return errorPage("This authorization request is not valid or has expired, start again from your MCP client.")

        return redirectToClient(request.redirectUri, request.state, "code" to code)
    }

    @PostMapping(CentralApiEndpoint.OAUTH_TOKEN, consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun token(
        @RequestParam("grant_type", required = false) grantType: String?,
        @RequestParam("client_id", required = false) clientId: String?,
        @RequestParam("code", required = false) code: String?,
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
        @RequestParam("code_verifier", required = false) codeVerifier: String?,
        @RequestParam("refresh_token", required = false) refreshToken: String?,
        @RequestParam("resource", required = false) resource: String?,
    ): ResponseEntity<Map<String, Any>> {
        if (clientId == null) {
            return tokenError("invalid_request", "client_id is required")
        }
        if (resource != null && resource.trimEnd('/') != mcpResource) {
            return tokenError("invalid_target", "Unknown resource")
        }

        val tokens: McpTokenPair? =
            when (grantType) {
                "authorization_code" -> {
                    if (code == null || redirectUri == null || codeVerifier == null) {
                        return tokenError("invalid_request", "code, redirect_uri and code_verifier are required")
                    }
                    if (codeVerifier.length !in MIN_CODE_VERIFIER_LENGTH..MAX_CODE_VERIFIER_LENGTH) {
                        return tokenError("invalid_request", "invalid code_verifier")
                    }
                    mcpOAuthService.exchangeCode(code, clientId, redirectUri, codeVerifier, resource?.trimEnd('/'))
                }

                "refresh_token" -> {
                    if (refreshToken == null) {
                        return tokenError("invalid_request", "refresh_token is required")
                    }
                    mcpOAuthService.refresh(refreshToken, clientId)
                }

                else -> return tokenError("unsupported_grant_type", "grant_type must be authorization_code or refresh_token")
            }

        if (tokens == null) {
            return tokenError("invalid_grant", "The code or refresh token is invalid, expired or was issued to another client")
        }

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(
                mapOf(
                    "access_token" to tokens.accessToken,
                    "token_type" to "Bearer",
                    "expires_in" to tokens.accessTokenTtl.seconds,
                    "refresh_token" to tokens.refreshToken,
                    "scope" to tokens.scopes.joinToString(" "),
                ),
            )
    }

    /**
     * Token introspection (RFC 7662) for the MCP server, so it can check the scopes and the audience of a token.
     * Anyone who has a token can already use it, so the endpoint does not need its own authentication.
     */
    @PostMapping(CentralApiEndpoint.OAUTH_INTROSPECT, consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun introspect(
        @RequestParam("token", required = false) token: String?,
    ): ResponseEntity<Map<String, Any>> {
        val introspection = token?.let { mcpOAuthService.introspect(it) }
        val body: Map<String, Any> =
            if (introspection == null) {
                mapOf("active" to false)
            } else {
                mapOf(
                    "active" to true,
                    "scope" to introspection.token.scopes.joinToString(" "),
                    "client_id" to introspection.token.clientId,
                    "sub" to introspection.token.userId,
                    "aud" to introspection.token.resource,
                    "exp" to Instant.now().epochSecond + introspection.expiresInSeconds,
                )
            }

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body)
    }

    /**
     * Token revocation (RFC 7009), it answers 200 also for unknown tokens.
     * Clients are public, so like introspection it does not need its own authentication.
     */
    @PostMapping(CentralApiEndpoint.OAUTH_REVOKE, consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun revoke(
        @RequestParam("token", required = false) token: String?,
    ): ResponseEntity<Void> {
        if (token != null) mcpOAuthService.revokeToken(token)
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build()
    }

    private fun tokenError(
        error: String,
        description: String,
    ): ResponseEntity<Map<String, Any>> =
        ResponseEntity
            .badRequest()
            .cacheControl(CacheControl.noStore())
            .body(mapOf("error" to error, "error_description" to description))

    private fun redirectToClient(
        redirectUri: String,
        state: String?,
        vararg params: Pair<String, String>,
    ): ResponseEntity<String> {
        val uri =
            UriComponentsBuilder
                .fromUriString(redirectUri)
                .also { builder ->
                    params.forEach { (key, value) -> builder.queryParam(key, value) }
                    if (state != null) builder.queryParam("state", state)
                    builder.queryParam("iss", issuer)
                }.build()
                .encode()
                .toUri()

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(uri)
            .cacheControl(CacheControl.noStore())
            .header("Referrer-Policy", "no-referrer")
            .build()
    }

    private fun errorPage(message: String) =
        htmlResponse(HttpStatus.BAD_REQUEST, "<h1>Authorization failed</h1><p>${HtmlUtils.htmlEscape(message)}</p>")

    private fun htmlResponse(
        status: HttpStatus,
        body: String,
    ): ResponseEntity<String> =
        ResponseEntity
            .status(status)
            .contentType(MediaType.TEXT_HTML)
            .cacheControl(CacheControl.noStore())
            .header("Referrer-Policy", "no-referrer")
            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'")
            .body(
                """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Astro</title>
                  <style>
                    body { font-family: system-ui, sans-serif; max-width: 32rem; margin: 4rem auto; padding: 0 1rem; line-height: 1.5; }
                    button { font-size: 1rem; padding: .5rem 1.25rem; margin-right: .5rem; }
                  </style>
                </head>
                <body>
                $body
                </body>
                </html>
                """.trimIndent(),
            )
}
