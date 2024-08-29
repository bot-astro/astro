package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import space.astro.shared.core.components.web.CentralApiRoutes

private val log = KotlinLogging.logger { }

@RestController
@Tag(name = "kubernetes")
class LifecycleController {

    @GetMapping(CentralApiRoutes.Kube.READY)
    suspend fun ready(): ResponseEntity<*> {
        log.info { "k8s requested ready status, responding with OK" }

        return ResponseEntity.ok().build<Any>()
    }

    @GetMapping(CentralApiRoutes.Kube.SHUTDOWN)
    suspend fun shutdown(): ResponseEntity<*> {
        log.info { "Got shutdown request" }

        // grace time for requests to finish since kubernetes doesn't route traffic to the pod anymore when requesting to shut down
        delay(3000)

        return ResponseEntity.ok().build<Any>()
    }
}
