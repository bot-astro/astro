package space.astro.api.central

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = [
    "space.astro.api.central.config",
    "space.astro.shared.core.config"
])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
