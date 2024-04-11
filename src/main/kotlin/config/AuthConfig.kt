package org.example.interviewtemplate.config

import kotlinx.coroutines.reactor.awaitSingleOrNull
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
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.ReactiveAuthorizationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authorization.AuthorizationContext
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.security.Principal


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
//            .addFilterAt(authWebFilter(), SecurityWebFiltersOrder.AUTHORIZATION)
//            .exceptionHandling {
//                // Research a bit more what this does when it populates the headers.
//                it.authenticationEntryPoint(BearerTokenServerAuthenticationEntryPoint())
//                it.accessDeniedHandler(BearerTokenServerAccessDeniedHandler())
//            }
            .authorizeExchange { auth ->
                auth.pathMatchers("api/auth/login").permitAll()
                auth.pathMatchers("api/auth/register").permitAll()
                //auth.anyExchange().authenticated()//.access(AuthorizationManager())
                auth.anyExchange().access(AuthorizationManager())
            }
            .authenticationManager(AuthenticationManager())
            .addFilterAt(authWebFilter(), SecurityWebFiltersOrder.REACTOR_CONTEXT)
            //.securityContextRepository(WebSessionServerSecurityContextRepository())
            .build()
    }

    private class BearerFilterAuth : CoWebFilter() {
        override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        }
    }

    private fun authWebFilter(): AuthenticationWebFilter {
        return AuthenticationWebFilter(AuthenticationManager()).apply {
            val failureHandler = ServerAuthenticationEntryPointFailureHandler(
                BearerTokenServerAuthenticationEntryPoint()
            )
            setAuthenticationFailureHandler(failureHandler)
            setAuthenticationSuccessHandler(WebFilterChainServerAuthenticationSuccessHandler())
            setServerAuthenticationConverter(ServerAuthenticationConverterImpl())
            setSecurityContextRepository(WebSessionServerSecurityContextRepository())
            setRequiresAuthenticationMatcher {
                ServerWebExchangeMatchers
                    .pathMatchers("api/movies/**", "api/users/**")
                    .matches(it)
            }
        }
    }

    @Bean
    fun userDetailsService(): ReactiveUserDetailsService {
        return ReactiveUserDetailsServiceImpl()
    }

    inner class ServerAuthenticationConverterImpl : ServerAuthenticationConverter {
        override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
            return mono {
                logger.debug { "ServerAuthenticationConverter is invoked." }
                val token = tryRetrieveToken(exchange.request.headers)
                val username = authService.authorize(token)
                logger.debug { "Verified token for user:$username" }
                val user = userRepository.findByName(username)
                    ?: throw AuthenticationException("User with username:$username, does not exists.")
                val principal = PrincipalWithUserId(user.name, user.id)
                AuthenticationImpl(principal, true)
            }
        }

        private fun tryRetrieveToken(headers: HttpHeaders): String {
            val authHeader = headers[HttpHeaders.AUTHORIZATION]
                ?: throw AuthenticationException("Bearer token is missing from headers.")
            val bearerToken = authHeader.firstOrNull() ?: throw AuthenticationException()
            val split = bearerToken.split(" ")
            return split.getOrNull(1) ?: throw AuthenticationException()
        }
    }


    inner class ReactiveUserDetailsServiceImpl : ReactiveUserDetailsService {
        override fun findByUsername(username: String): Mono<UserDetails> {
            return mono {
                logger.debug { "Authenticating username:$username." }
                val user = userRepository.findByName(username)
                    ?: throw AuthenticationException()
                UserDetailsImpl(user.name, user.encryptedPassword)
            }
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

    inner class AuthorizationManager : ReactiveAuthorizationManager<AuthorizationContext> {
        override fun check(
            authentication: Mono<Authentication>,
            `object`: AuthorizationContext
        ): Mono<AuthorizationDecision> = mono {
            logger.debug { "AuthorizeManager is invoked." }
            authentication.awaitSingleOrNull()?.let {
                // We check for already authenticated clients.
                // The typical use-case for this is to allow clients
                // to use `@WithMockUser` or similar annotations to
                // skip authentication.
                if (it.isAuthenticated) return@mono AuthorizationDecision(true)
            }
            val token = tryRetrieveToken(`object`)
            val username = authService.authorize(token)
            logger.debug { "Verified token for user:$username" }
            val user = userRepository.findByName(username)
                ?: throw AuthenticationException("User with username:$username, does not exists.")
            // This is not used anywhere.
            // `object`.exchange.mutate().principal(createPrincipal(user.name, user.id).toMono()).build()
            AuthorizationDecision(true)
        }

        private fun tryRetrieveToken(auth: AuthorizationContext): String {
            val authHeader = auth
                .exchange
                .request
                .headers[HttpHeaders.AUTHORIZATION]
                ?: throw AuthenticationException("Bearer token is missing from headers.")
            val bearerToken = authHeader.firstOrNull() ?: throw AuthenticationException()
            val split = bearerToken.split(" ")
            return split.getOrNull(1) ?: throw AuthenticationException()
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

private class PrincipalWithUserId(
    private val username: String,
    private val userId: Int
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

private class AuthenticationImpl(
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