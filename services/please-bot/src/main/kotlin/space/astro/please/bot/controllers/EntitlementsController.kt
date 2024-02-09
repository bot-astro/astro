package space.astro.please.bot.controllers

import net.dv8tion.jda.api.entities.entitlement.Entitlement
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/please")
class EntitlementsController {

    @GetMapping("/work")
    suspend fun entitlementCreated(@RequestBody entitlement: Entitlement): ResponseEntity<*> {
        return ResponseEntity.ok("it works")
    }
}