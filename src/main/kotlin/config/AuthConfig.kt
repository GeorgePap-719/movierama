package org.example.interviewtemplate.config

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.example.interviewtemplate.repositories.UserRepository
import org.example.interviewtemplate.services.AuthService
import org.example.interviewtemplate.services.AuthenticationException
import org.example.interviewtemplate.util.debug
import org.example.interviewtemplate.util.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.ReactiveAuthorizationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler
import org.springframework.security.web.server.authorization.AuthorizationContext
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.security.Principal


@Configuration
@EnableWebFluxSecurity
class AuthConfig(
    private val authService: AuthService,
    private val userRepository: UserRepository
) {
    private val logger = logger()

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    fun apiHttpSecurity(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { csrf -> csrf.disable() }
            .cors {
                it.configurationSource { CorsConfiguration().apply { applyPermitDefaultValues() } }
            }
            .authorizeExchange { auth ->
                auth.pathMatchers("api/auth/login").permitAll()
                auth.pathMatchers("api/auth/register").permitAll()
                auth.anyExchange().access(AuthorizationManager())
            }
            .authenticationManager(AuthenticationManager())
            .addFilterAt(authWebFilter(), SecurityWebFiltersOrder.REACTOR_CONTEXT)
            .build()
    }

    private fun authWebFilter(): AuthenticationWebFilter {
        return AuthenticationWebFilter(AuthenticationManager()).apply {
            setRequiresAuthenticationMatcher {
                ServerWebExchangeMatchers
                    .pathMatchers("api/movies/**", "api/users/**")
                    .matches(it)
            }
            val failureHandler = ServerAuthenticationEntryPointFailureHandler(
                BearerTokenServerAuthenticationEntryPoint()
            )
            setAuthenticationFailureHandler(failureHandler)
            setServerAuthenticationConverter(ServerAuthenticationConverterImpl())
        }
    }

    inner class ServerAuthenticationConverterImpl : ServerAuthenticationConverter {
        override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
            return mono {
                val request = exchange.request
                // Add exception for GET `/api/movies` path.
                if (request.path.toString() == "/api/movies" && request.method == HttpMethod.GET) {
                    return@mono AuthenticationToken(null, true)
                }
                val token = tryRetrieveToken(exchange.request.headers)
                val username = authService.authorize(token)
                logger.debug { "Verified token for user:$username" }
                val user = userRepository.findByName(username)
                    ?: throw AuthenticationException("User with username:$username, does not exists.")
                val principal = PrincipalWithUserId(user.name, user.id)
                AuthenticationToken(principal, true)
            }
        }

        private fun tryRetrieveToken(headers: HttpHeaders): String {
            val authHeader = headers[HttpHeaders.AUTHORIZATION]
                ?: throw AuthenticationException("Bearer token is missing from headers.")
            val bearerToken = authHeader.firstOrNull()
                ?: throw AuthenticationException(malformedTokenMsg)
            val split = bearerToken.split(" ")
            return split.getOrNull(1)
                ?: throw AuthenticationException(malformedTokenMsg)
        }

        private val malformedTokenMsg = "Bearer token is probably malformed."
    }

    inner class AuthorizationManager : ReactiveAuthorizationManager<AuthorizationContext> {
        override fun check(
            authentication: Mono<Authentication>,
            `object`: AuthorizationContext
        ): Mono<AuthorizationDecision> = mono {
            authentication.awaitSingleOrNull()?.let {
                // We check for already authenticated clients.
                // The typical use-case for this is to allow clients
                // to use `@WithMockUser` or similar annotations to
                // skip authentication.
                if (it.isAuthenticated) return@mono AuthorizationDecision(true)
            }
            // At this point, even if we can authenticate the request,
            // we cannot populate the `principal` property.
            // It's more convenient to deny any requests that reach here.
            AuthorizationDecision(false)
        }
    }

    inner class AuthenticationManager : ReactiveAuthenticationManager {
        override fun authenticate(authentication: Authentication): Mono<Authentication> {
            return mono {
                logger.debug { "Authentication manager is invoked" }
                if (authentication.isAuthenticated) return@mono authentication
                throw AuthenticationException("Request is not authenticated.")
            }
        }
    }
}

class PrincipalWithUserId(
    private val username: String,
    val userId: Int
) : Principal {
    override fun getName(): String = username

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PrincipalWithUserId
        if (username != other.username) return false
        if (userId != other.userId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + userId
        return result
    }
}

class AuthenticationToken(
    val principalWithUserId: PrincipalWithUserId? = null,
    private var authenticated: Boolean
) : Authentication {
    override fun getName(): String = principalWithUserId?.name ?: "UNKNOWN"
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = ArrayList()
    override fun getCredentials(): Any? = null
    override fun getDetails(): Any? = null
    override fun getPrincipal(): PrincipalWithUserId? = principalWithUserId
    override fun isAuthenticated(): Boolean = authenticated
    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }
}