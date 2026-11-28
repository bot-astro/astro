package space.astro.api.central.services

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import space.astro.api.central.models.mcp.McpClientMetadata
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.InetAddress
import java.net.URI
import java.time.Duration

class InvalidClientMetadataException(
    message: String,
) : Exception(message)

/**
 * Fetches and validates the Client ID Metadata Documents of MCP clients,
 * the `client_id` of those clients is the HTTPS URL of their metadata document
 */
@Service
class McpClientMetadataService(
    private val jsonMapper: JsonMapper,
) {
    private companion object {
        const val MAX_DOCUMENT_BYTES = 10 * 1024
        val FETCH_TIMEOUT: Duration = Duration.ofSeconds(5)
    }

    // The addresses are checked by the DNS lookup of the client itself, so the connection can only go to
    // the addresses that were checked (a second lookup could return another address, DNS rebinding)
    private val httpClient =
        OkHttpClient
            .Builder()
            .dns(
                object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> = resolvePublicAddresses(hostname)
                },
            ).followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(FETCH_TIMEOUT)
            .build()

    /**
     * @throws InvalidClientMetadataException if the client id is not a valid metadata document URL,
     * the document can't be fetched or its content is not valid
     */
    fun fetch(clientId: String): McpClientMetadata {
        val uri = parseClientIdUri(clientId)
        // OkHttp does not look up the hosts that are IP addresses
        resolvePublicAddresses(uri.host)

        val body =
            try {
                httpClient
                    .newCall(
                        Request
                            .Builder()
                            .url(uri.toString())
                            .header("Accept", "application/json")
                            .build(),
                    ).execute()
                    .use { response ->
                        if (response.code != 200) {
                            throw InvalidClientMetadataException("The client metadata document returned status ${response.code}")
                        }
                        val bytes = response.body.byteStream().readNBytes(MAX_DOCUMENT_BYTES + 1)
                        if (bytes.size > MAX_DOCUMENT_BYTES) {
                            throw InvalidClientMetadataException("The client metadata document is too large")
                        }
                        bytes
                    }
            } catch (e: InvalidClientMetadataException) {
                throw e
            } catch (e: Exception) {
                throw InvalidClientMetadataException("Could not fetch the client metadata document: ${e.javaClass.simpleName}")
            }

        val json =
            try {
                jsonMapper.readTree(body)
            } catch (e: Exception) {
                throw InvalidClientMetadataException("The client metadata document is not valid JSON")
            }

        if (json.path("client_id").asString() != clientId) {
            throw InvalidClientMetadataException("The client_id of the metadata document does not match its URL")
        }

        val authMethod = json.path("token_endpoint_auth_method").asString("none")
        if (authMethod != "none") {
            throw InvalidClientMetadataException("Only public clients are supported (token_endpoint_auth_method none)")
        }

        return McpClientMetadata(
            clientId = clientId,
            clientName =
                json
                    .path("client_name")
                    .asString("")
                    .trim()
                    .take(100)
                    .ifEmpty { uri.host },
            redirectUris = readRedirectUris(json),
        )
    }

    private fun parseClientIdUri(clientId: String): URI {
        val uri =
            try {
                URI(clientId)
            } catch (e: Exception) {
                throw InvalidClientMetadataException("The client_id is not a valid URL")
            }

        val pathSegments = uri.path?.split("/").orEmpty()
        val validOrigin = uri.scheme == "https" && !uri.host.isNullOrEmpty() && uri.userInfo == null
        val validPath = !uri.path.isNullOrEmpty() && uri.path != "/" && pathSegments.none { it == "." || it == ".." }
        if (!validOrigin || uri.fragment != null || !validPath) {
            throw InvalidClientMetadataException(
                "The client_id must be an https URL with a path, without credentials, fragment or dot segments",
            )
        }
        return uri
    }

    private fun resolvePublicAddresses(host: String): List<InetAddress> {
        val addresses =
            try {
                InetAddress.getAllByName(host)
            } catch (e: Exception) {
                throw InvalidClientMetadataException("The host of the client_id can't be resolved")
            }

        if (addresses.any { isLocalAddress(it) || it.isMulticastAddress || isUniqueLocalIpv6(it) }) {
            throw InvalidClientMetadataException("The host of the client_id is not public")
        }

        return addresses.toList()
    }

    private fun isLocalAddress(address: InetAddress) =
        address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress

    private fun isUniqueLocalIpv6(address: InetAddress) = address.address.size == 16 && (address.address[0].toInt() and 0xfe) == 0xfc

    private fun readRedirectUris(json: JsonNode): List<String> {
        val redirectUris =
            json
                .path("redirect_uris")
                .values()
                .map { it.asString("") }
                .filter { it.isNotEmpty() }

        if (redirectUris.isEmpty()) {
            throw InvalidClientMetadataException("The client metadata document has no redirect_uris")
        }

        redirectUris.forEach {
            val uri = runCatching { URI(it) }.getOrNull()
            val isLoopback = uri?.host == "localhost" || uri?.host == "127.0.0.1" || uri?.host == "[::1]"
            // https, http on loopback for local clients, or the custom scheme of a native app
            val isAllowedScheme =
                when (uri?.scheme) {
                    null, "javascript", "data", "file" -> false
                    "https" -> true
                    "http" -> isLoopback
                    else -> true
                }
            if (uri == null || uri.fragment != null || !isAllowedScheme) {
                throw InvalidClientMetadataException("Invalid redirect URI: $it")
            }
        }

        return redirectUris
    }
}
