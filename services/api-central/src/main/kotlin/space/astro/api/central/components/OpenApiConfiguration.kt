package space.astro.api.central.components

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.astro.shared.core.exceptions.AErrorCode

@Configuration
class OpenApiConfiguration {

    companion object {
        const val SESSION_SECURITY_NAME = "session"
        const val ERROR_RESPONSE_SCHEMA = "#/components/schemas/AErrorResponse"
    }

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Astro API")
                    .version("2.0")
                    .description("Astro REST API, mainly used by the web dashboard.")
            )
            .components(
                Components()
                    .addSchemas(
                        "AErrorResponse",
                        ObjectSchema()
                            .description("API error details.")
                            .addProperty(
                                "code",
                                StringSchema()
                                    .apply { AErrorCode.entries.forEach { addEnumItem(it.name) } }
                                    .description("Application error code identifying the failure.")
                            )
                            .addProperty(
                                "message",
                                StringSchema()
                                    .nullable(true)
                                    .description("Human-readable description of the error; may be null.")
                            )
                            .required(listOf("code"))
                            .example(
                                mapOf(
                                    "code" to "NOT_FOUND",
                                    "message" to "Guild with id 123456789 not found"
                                )
                            )
                    )
                    .addSecuritySchemes(
                        "sessionCookie",
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.COOKIE)
                            .name(SESSION_SECURITY_NAME)
                    )
            )
    }
}