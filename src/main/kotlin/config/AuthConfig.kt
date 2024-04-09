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
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class AuthConfig(
    private val authService: AuthService,
    @Value("\${sharedkey}")
    private val _sharedKey: String,
    private val userRepository: UserRepository
) {
    private val sharedKey = _sharedKey.toByteArray()
    private val logger = logger()

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    fun defaultSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http {
            authorizeExchange {
                authorize(anyExchange, authenticated)
            }
            formLogin { }
            httpBasic { }
        }
    }

    @Bean
    fun userDetailsService(): ReactiveUserDetailsService {
        return ReactiveUserDetailsService {
            return@ReactiveUserDetailsService mono {
                val user = userRepository.findByName(it)
                    ?: throw AuthenticationException()
                UserDetailsImpl(user.name, user.encryptedPassword)
            }
        }
    }

    inner class ServerAuthenticationConverterImpl : ServerAuthenticationConverter {
        override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
            val headers = exchange.request.headers
            val bearerToken = headers["Authorization"]
                ?: throw AuthenticationException()

            println("--- bearerToken:$bearerToken ---")
            TODO()
        }
    }

    inner class ReactiveAuthenticationManagerImpl : ReactiveAuthenticationManager {
        override fun authenticate(authentication: Authentication): Mono<Authentication> {
            return mono {
                if (authentication.isAuthenticated) return@mono authentication
                val username = authentication.name
                val password = authentication.credentials
                logger.debug { "Authenticating user with username:$username, password:$password." }
//                service.login()
                TODO("Not yet implemented")
            }
        }

    }
}

object JwtDecoderImpl : ReactiveJwtDecoder {
    override fun decode(token: String): Mono<Jwt> {
        return mono { Jwt.withTokenValue(token).build() }
    }
}

class UserDetailsImpl(
    private val username: String,
    private val password: String,
    private val authorities: MutableCollection<out GrantedAuthority> = ArrayList()
) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return authorities
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }

}