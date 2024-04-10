package org.example.interviewtemplate.config

import kotlinx.coroutines.reactor.mono
import org.example.interviewtemplate.repositories.UserRepository
import org.example.interviewtemplate.services.AuthService
import org.example.interviewtemplate.services.AuthenticationException
import org.example.interviewtemplate.util.debug
import org.example.interviewtemplate.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.ReactiveAuthorizationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authorization.AuthorizationContext
import reactor.core.publisher.Mono


@Configuration
@EnableWebFluxSecurity
class AuthConfig(
    private val authService: AuthService,
    @Value("\${sharedkey}")
    private val _sharedKey: String,
    private val userRepository: UserRepository
) {
    private val logger = logger()

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    fun apiHttpSecurity(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { csrf -> csrf.disable() }
            .authorizeExchange { auth ->
                auth.pathMatchers("api/auth/login").permitAll()
                auth.pathMatchers("api/users/register").permitAll()
                auth.anyExchange().access(AuthManager())
            }.build()
    }


    @Bean
    fun userDetailsService(): ReactiveUserDetailsService {
        return ReactiveUserDetailsServiceImpl()
    }

    inner class ReactiveUserDetailsServiceImpl : ReactiveUserDetailsService {
        override fun findByUsername(username: String): Mono<UserDetails> {
            return mono {
                val user = userRepository.findByName(username)
                    ?: throw AuthenticationException()
                UserDetailsImpl(user.name, user.encryptedPassword)
            }
        }
    }

    inner class AuthManager : ReactiveAuthorizationManager<AuthorizationContext> {
        override fun check(
            authentication: Mono<Authentication>,
            `object`: AuthorizationContext
        ): Mono<AuthorizationDecision> = mono {
            val token = tryRetrieveToken(`object`)
            val authorized = authService.tryAuthorize(token)
            AuthorizationDecision(authorized)
        }

        private fun tryRetrieveToken(auth: AuthorizationContext): String {
            val authHeader = auth
                .exchange
                .request
                .headers[HttpHeaders.AUTHORIZATION]
                ?: throw AuthenticationException()
            val bearerToken = authHeader.firstOrNull() ?: throw AuthenticationException()
            val split = bearerToken.split(" ")
            return split.getOrNull(1) ?: throw AuthenticationException()
        }

        private fun AuthService.tryAuthorize(token: String): Boolean {
            return try {
                authorize(token)
                true
            } catch (e: AuthenticationException) {
                logger.debug { e.stackTraceToString() }
                false
            }
        }
    }
}

class UserDetailsImpl(
    private val username: String,
    private val password: String,
    private val authorities: MutableCollection<out GrantedAuthority> = ArrayList()
) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = authorities
    override fun getPassword(): String = password
    override fun getUsername(): String = username
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}