// exports the OpenAPI spec with `generateOpenApiDocs`, booting the service with the `openapi` profile
plugins {
    id("service-conventions")
    id("org.springdoc.openapi-gradle-plugin")
}

openApi {
    customBootRun {
        args = listOf("--spring.profiles.active=openapi")
    }
}

listOf("generateOpenApiDocs", "forkedSpringBootRun", "forkedSpringBootStop").forEach {
    tasks.named(it) { notCompatibleWithConfigurationCache("the springdoc plugin tasks reference other tasks") }
}
