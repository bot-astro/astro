package space.astro.support.bot.api.components

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    companion object {
        const val AUTH_SECURITY_NAME = "auth"
    }

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Astro Support Bot API")
                    .version("2.0")
                    .description("Astro support bot API, used internally to react to premium events in the support server."),
            ).components(
                Components()
                    .addSecuritySchemes(
                        AUTH_SECURITY_NAME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name("Authorization"),
                    ),
            ).addSecurityItem(SecurityRequirement().addList(AUTH_SECURITY_NAME))
}
