package space.astro.api.central.models.auth

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class AuthContext(
    private val sessionData: AuthSession,
    private val authorities: Collection<GrantedAuthority>
) : Authentication {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getCredentials(): Any? = null

    override fun getDetails(): Any? = null

    override fun getPrincipal(): AuthSession = sessionData

    override fun isAuthenticated(): Boolean = true

    override fun setAuthenticated(isAuthenticated: Boolean) {}

    override fun getName(): String = "User ${sessionData.userId}"
}