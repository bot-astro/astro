package space.astro.api.central.components.middlewares

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Middleware that makes sure the authenticated user of the request
 * has enough Discord permissions on the target guild to manage it.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@guildPermissionsHelper.authedUserCanManageGuild(authentication.principal, #guildId)")
annotation class CanManageGuildMiddleware
