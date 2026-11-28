package space.astro.api.central.components

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.core.util.Json31
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
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.providers.ObjectMapperProvider
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

        /** Set on the PATCH operations with the name of the schema they patch, see [schemasCustomizer] */
        const val MERGE_PATCH_EXTENSION = "x-merge-patch-of"

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

    /**
     * The API (de)serializes JSON with snake_case names, springdoc would document them in camelCase
     * and the generated tools would send fields the API does not know
     */
    @Bean
    fun snakeCaseModelResolver(objectMapperProvider: ObjectMapperProvider): ModelResolver =
        ModelResolver(objectMapperProvider.jsonMapper().copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE))

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
                        Everything except the login and the webhooks needs the session cookie that the Discord OAuth login sets, or an
                        `Authorization: Bearer` access token issued by the OAuth server of this API (used by MCP clients). Requests
                        without a valid session or token fail with 401 `UNAUTHENTICATED`.
                        Endpoints with a `guildId` also require that the logged in user can manage that guild on Discord, otherwise they fail
                        with 403 `UNAUTHORIZED`.
                        Access tokens have scopes: `guilds:read` for `GET` requests and `guilds:write` for everything else (it includes
                        read). A token without the needed scope fails with 403 `INSUFFICIENT_SCOPE`.

                        ## Errors
                        Errors are JSON objects with a machine readable `code`, a human readable `message`, the `param` that caused the error
                        when there is one and the `request_id`, see `AErrorResponse`. Rate limited requests (429) have a `Retry-After`
                        header with the seconds to wait.

                        ## Request ids
                        Every response has an `X-Request-Id` header, the same id is in the `request_id` of errors. Give it when reporting a
                        problem. A request can send its own `X-Request-Id` (8 to 128 letters, digits, `.`, `_` or `-`).

                        ## Retrying writes
                        `POST` and `PATCH` requests accept an `Idempotency-Key` header (any unique string up to 255 characters, like a UUID).
                        The first successful response is stored for 24 hours and sent again, with an `Idempotent-Replayed: true` header,
                        when the same key is repeated, so a retry never repeats the change. Reusing a key for a different request fails
                        with 422 `IDEMPOTENCY_KEY_REUSED`, repeating it while the first request is still running fails with 409
                        `IDEMPOTENCY_IN_PROGRESS` (retry after the `Retry-After` seconds). Failed requests are not stored, retry them
                        with the same key. Keys do not work on multipart requests.

                        ## Partial updates and dry runs
                        The `PATCH` update endpoints change only the fields of the body, as a JSON Merge Patch: nested objects are merged
                        field by field, lists are replaced as a whole, and a null field resets it to its default. They also accept
                        `dryRun=true`: all the checks run (validation, premium, existence) and the result is returned but nothing is saved or
                        changed on Discord. Other endpoints reject `dryRun=true` with 400.

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
                                            "INSUFFICIENT_SCOPE: the access token lacks the guilds:read or guilds:write scope. " +
                                            "IDEMPOTENCY_KEY_REUSED: the Idempotency-Key was used for a different request. " +
                                            "IDEMPOTENCY_IN_PROGRESS: a request with the same Idempotency-Key is still running. " +
                                            "BOT_NOT_IN_GUILD: Astro is not a member of the guild, the user has to invite it first. " +
                                            "UNKNOWN and TRANSLATION: unexpected server failures.",
                                    ),
                            ).addProperty(
                                "message",
                                StringSchema()
                                    .nullable(true)
                                    .description("Human-readable description of the error, safe to show to the user; may be null."),
                            ).addProperty(
                                "param",
                                StringSchema()
                                    .nullable(true)
                                    .description("Name of the body field (dotted path) or query parameter that caused the error, if any."),
                            ).addProperty(
                                "request_id",
                                StringSchema()
                                    .nullable(true)
                                    .description("Id of the failed request, the same as the X-Request-Id response header."),
                            ).required(listOf("code"))
                            .example(
                                mapOf(
                                    "code" to "NOT_FOUND",
                                    "message" to "Guild with id 123456789 not found",
                                    "request_id" to "req_0f8e2c1d4b6a4e1f9c3d7a5b2e8f1c0d",
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
     * generated clients and tools would treat every field of the responses as optional.
     *
     * Then it gives the PATCH operations (marked with `x-merge-patch-of`) a body schema where nothing is required.
     */
    @Bean
    fun schemasCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val schemas = openApi.components?.schemas ?: return@OpenApiCustomizer
            schemas.values.forEach { schema ->
                if (schema.required.isNullOrEmpty() && !schema.properties.isNullOrEmpty()) {
                    schema.required =
                        schema.properties
                            .filter { (_, property) -> property.types?.contains("null") != true }
                            .keys
                            .toList()
                }
            }

            openApi.paths?.values?.flatMap { it.readOperations() }?.forEach { operation ->
                val patchedSchema = operation.extensions?.remove(MERGE_PATCH_EXTENSION) as? String ?: return@forEach
                val patchSchemaName = createPatchSchema(schemas, patchedSchema)
                operation.requestBody =
                    RequestBody()
                        .required(true)
                        .content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/$patchSchemaName")),
                            ),
                        )
            }
        }

    /**
     * Copies the schema [name] and the object schemas of its properties with no required property, because
     * a merge patch can send any subset of the fields (also of the nested objects). Lists are not copied
     * since a patch replaces them as a whole.
     *
     * @return the name of the copy
     */
    private fun createPatchSchema(
        schemas: MutableMap<String, Schema<*>>,
        name: String,
    ): String {
        val patchName = "${name}Patch"
        if (schemas.containsKey(patchName)) return patchName

        val mapper = Json31.mapper()
        val copy = mapper.readValue(mapper.writeValueAsString(schemas.getValue(name)), Schema::class.java)
        schemas[patchName] = copy
        copy.required = null
        copy.description =
            "Any subset of the fields of `$name`. Fields that are left out are not changed, null resets a field to its default."
        copy.properties?.values?.forEach { property ->
            val ref = property.`$ref`?.substringAfterLast('/') ?: return@forEach
            if (!schemas[ref]?.properties.isNullOrEmpty()) {
                property.`$ref` = "#/components/schemas/${createPatchSchema(schemas, ref)}"
            }
        }
        return patchName
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
