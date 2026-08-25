package space.astro.api.central.components

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    companion object {
        const val SESSION_SECURITY_NAME = "session"
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