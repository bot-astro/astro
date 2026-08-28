package space.astro.bot.api.components

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    companion object {
        const val TOKEN_SECURITY_NAME = "token"
    }

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Astro Bot API")
                    .version("2.0")
                    .description("Astro bot API, used internally to perform actions on Discord on behalf of the bot.")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "auth_token",
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name(TOKEN_SECURITY_NAME)
                    )
            )
    }
}