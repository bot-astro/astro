package space.astro.api.central.components

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.astro.shared.core.exceptions.AErrorCode

@Configuration
class OpenApiConfiguration {
    companion object {
        const val SESSION_SECURITY_NAME = "session"
        const val CHARGEBEE_WEBHOOK_SECURITY_NAME = "chargebeeWebhook"
        const val ERROR_RESPONSE_SCHEMA = "#/components/schemas/AErrorResponse"
        const val WEBHOOKS_TAG = "Webhooks"

        private val PARAMETER_DESCRIPTIONS =
            mapOf(
                "guildId" to
                    ("Id of the Discord guild (server). The user must be allowed to manage it." to "1234567890123456789"),
                "generatorId" to
                    ("Id of the generator, which is the id of its Discord voice channel." to "1234567890123456789"),
                "interfaceId" to
                    (
                        "Id of the interface, which is the id of its Discord message. " +
                            "It changes when the interface is moved to another channel." to "1234567890123456789"
                    ),
                "connectionId" to
                    ("Id of the connection, which is the id of the Discord voice channel it is attached to." to "1234567890123456789"),
                "templateId" to ("Id of the template, generated when the template is created." to "V1StGXR8_Z5jdHi6B-myT"),
                "mediaId" to ("Id of the uploaded media." to "665f1c2e8a3b4c0001a1b2c3"),
            )
    }

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Astro API")
                    .version("2.0")
                    .description(
                        """
                        REST API of Astro, a Discord bot that creates temporary voice channels (VCs), assigns roles to people in voice channels
                        and lets members control their VC with an interface message.

                        It is used by the web dashboard to configure the bot of a Discord guild (server).

                        ## Authentication
                        Everything except the login and the webhooks needs the session cookie that the Discord OAuth login sets. Requests
                        without a valid session fail with 401 `UNAUTHENTICATED`.
                        Endpoints with a `guildId` also require that the logged in user can manage that guild on Discord, otherwise they fail
                        with 403 `UNAUTHORIZED`.

                        ## Errors
                        Errors are JSON objects with a machine readable `code` and a human readable `message`, see `AErrorResponse`. Rate
                        limited requests (429) have a `Retry-After` header with the seconds to wait.

                        ## Premium
                        Some actions are limited on free guilds, they fail with 403 `PREMIUM_REQUIRED` and a `message` that says what to
                        upgrade.

                        ## Ids
                        Discord ids (guilds, channels, roles, users, messages) are snowflakes: numeric strings of 17 to 20 digits, never
                        numbers.
                        """.trimIndent(),
                    ),
            ).tags(
                listOf(
                    Tag().name("Authentication").description("Discord login and the logged in user."),
                    Tag()
                        .name(
                            "Discord",
                        ).description("Read-only views of Discord data: the user's guilds, and the channels and roles of a guild."),
                    Tag().name("Discord constants").description("Static Discord reference data."),
                    Tag().name("Guild settings").description("Settings of a guild: language, bot profile, caches."),
                    Tag().name("Setup").description("One-call initial setup of Astro in a guild."),
                    Tag()
                        .name(
                            "Generators settings",
                        ).description("Generators are voice channels that create a temporary VC when someone joins them."),
                    Tag()
                        .name(
                            "Interfaces settings",
                        ).description("Interfaces are messages with buttons that members use to control their temporary VC."),
                    Tag()
                        .name(
                            "Connections settings",
                        ).description("Voice roles: roles assigned to members while they are in a voice channel."),
                    Tag().name("Templates settings").description("Saved presets of temporary VC settings that members can apply."),
                    Tag().name("Updates channel settings").description("The channel where Astro posts announcements and setup alerts."),
                    Tag().name("Premium settings").description("Upgrade or downgrade a guild to premium."),
                    Tag()
                        .name(
                            "Guild errors",
                        ).description("Configuration errors that Astro hit while running in a guild, for example missing permissions."),
                    Tag().name("Media").description("Images uploaded for the bot profile and interfaces."),
                    Tag().name("Chargebee").description("Premium subscriptions of the logged in user."),
                    Tag().name(WEBHOOKS_TAG).description("Called by external services, not by users."),
                ),
            ).components(
                Components()
                    .addSchemas(
                        "AErrorResponse",
                        ObjectSchema()
                            .description("Error returned by every endpoint when a request fails.")
                            .addProperty(
                                "code",
                                StringSchema()
                                    .apply { AErrorCode.entries.forEach { addEnumItem(it.name) } }
                                    .description(
                                        "Application error code identifying the failure. " +
                                            "INVALID_REQUEST: malformed or invalid input. " +
                                            "UNAUTHENTICATED: no valid session. " +
                                            "UNAUTHORIZED: the user is not allowed to do this. " +
                                            "NOT_FOUND: the target does not exist. " +
                                            "PREMIUM_REQUIRED: the guild needs premium for this. " +
                                            "TOO_MANY_REQUESTS: rate limited, see the Retry-After header. " +
                                            "BOT_NOT_IN_GUILD: Astro is not a member of the guild, the user has to invite it first. " +
                                            "UNKNOWN and TRANSLATION: unexpected server failures.",
                                    ),
                            ).addProperty(
                                "message",
                                StringSchema()
                                    .nullable(true)
                                    .description("Human-readable description of the error, safe to show to the user; may be null."),
                            ).required(listOf("code"))
                            .example(
                                mapOf(
                                    "code" to "NOT_FOUND",
                                    "message" to "Guild with id 123456789 not found",
                                ),
                            ),
                    ).addSecuritySchemes(
                        SESSION_SECURITY_NAME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.COOKIE)
                            .name(SESSION_SECURITY_NAME)
                            .description("Session cookie set by the Discord OAuth login."),
                    ).addSecuritySchemes(
                        CHARGEBEE_WEBHOOK_SECURITY_NAME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("basic")
                            .description("Basic auth with the Chargebee webhook token as the password."),
                    ),
            ).addSecurityItem(SecurityRequirement().addList(SESSION_SECURITY_NAME))

    /**
     * Springdoc does not mark the non-null Kotlin properties as required,
     * generated clients and tools would treat every field of the responses as optional
     */
    @Bean
    fun requiredPropertiesCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.components?.schemas?.values?.forEach { schema ->
                if (schema.required.isNullOrEmpty() && !schema.properties.isNullOrEmpty()) {
                    schema.required =
                        schema.properties
                            .filter { (_, property) -> property.types?.contains("null") != true }
                            .keys
                            .toList()
                }
            }
        }

    /**
     * Describes the path parameters and adds the error responses that every endpoint can return,
     * so that they don't have to be repeated on each operation
     */
    @Bean
    fun operationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            operation.parameters?.forEach { parameter ->
                PARAMETER_DESCRIPTIONS[parameter.name]?.let { (description, example) ->
                    if (parameter.description == null) parameter.description = description
                    if (parameter.example == null) parameter.example = example
                }
            }

            val isPublic = handlerMethod.hasMethodAnnotation(SecurityRequirements::class.java)
            if (isPublic || operation.tags?.contains(WEBHOOKS_TAG) == true) {
                return@OperationCustomizer operation
            }

            val responses = operation.responses ?: ApiResponses().also { operation.responses = it }

            fun addError(
                code: String,
                description: String,
                retryAfter: Boolean = false,
            ) {
                if (responses[code] != null) return
                responses.addApiResponse(
                    code,
                    ApiResponse()
                        .description(description)
                        .content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().schema(Schema<Any>().`$ref`(ERROR_RESPONSE_SCHEMA)),
                            ),
                        ).apply {
                            if (retryAfter) {
                                addHeaderObject(
                                    "Retry-After",
                                    Header().description("Seconds to wait before retrying.").schema(IntegerSchema()),
                                )
                            }
                        },
                )
            }

            if (operation.requestBody != null) {
                addError("400", "The request body or a parameter is invalid (`INVALID_REQUEST`). The `message` says what is wrong.")
            }
            addError("401", "No valid session (`UNAUTHENTICATED`), the user has to log in again.")
            if (operation.parameters?.any { it.name == "guildId" } == true) {
                addError(
                    "403",
                    "The user cannot manage the guild (`UNAUTHORIZED`), or the guild needs premium for this (`PREMIUM_REQUIRED`).",
                )
                addError(
                    "404",
                    "The guild or the target of the request was not found (`NOT_FOUND`), or Astro is not in the guild " +
                        "(`BOT_NOT_IN_GUILD`).",
                )
            }
            addError("429", "Too many requests (`TOO_MANY_REQUESTS`), retry after the `Retry-After` seconds.", retryAfter = true)
            addError("500", "Unexpected server error (`UNKNOWN`).")

            operation
        }
}
