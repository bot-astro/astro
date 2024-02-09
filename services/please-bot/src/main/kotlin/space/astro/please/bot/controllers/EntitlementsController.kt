package space.astro.please.bot.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/please")
class EntitlementsController {

    @GetMapping("/work")
    suspend fun entitlementCreated(): ResponseEntity<*> {
        return ResponseEntity.ok("it works")
    }
}