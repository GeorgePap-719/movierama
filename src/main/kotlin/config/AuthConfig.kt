package org.example.interviewtemplate.config

import kotlinx.coroutines.reactor.mono
import org.example.interviewtemplate.repositories.UserRepository
import org.example.interviewtemplate.services.AuthService
import org.example.interviewtemplate.services.AuthenticationException
import org.example.interviewtemplate.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.server.SecurityWebFilterChain
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
                auth.pathMatchers("/login").permitAll()
                auth.pathMatchers("api/users/register").permitAll()
                auth.anyExchange().authenticated()
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

class AuthManager : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
        TODO("Not yet implemented")
    }
}