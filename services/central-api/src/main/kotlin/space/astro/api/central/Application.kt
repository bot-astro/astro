package space.astro.api.central

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.api.central",
        "space.astro.shared.core.configs",
        "space.astro.shared.core.components.io",
        "space.astro.shared.core.components.managers",
        "space.astro.shared.core.services.redis",
        "space.astro.shared.core.components.kmongo",
        "space.astro.shared.core.components.coroutine",
        "space.astro.shared.core.services.chargebee",
        "space.astro.shared.core.services.bot",
        "space.astro.shared.core.services.support",
        "space.astro.shared.core.daos",
        "space.astro.shared.core.services.discord",
    ]
)
@OpenAPIDefinition(
    info = Info(
        title = "Astro central API",
        description = "API that external services use to interact with Astro",
        termsOfService = "https://astro-bot.space/terms",
        contact = Contact(
            name = "Astro Bot",
            url = "https://astro-bot.space",
            email = "hi@astro-bot.space"
        )
    ),
    servers = [Server(
        url = "https://api.astro-bot.space",
        description = "Production"
    ), Server(
        url = "http://localhost:9001",
        description = "Local development"
    )],

)
class Application

// build
fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
