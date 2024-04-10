package org.example.interviewtemplate.api

import org.example.interviewtemplate.dto.LoginUser
import org.example.interviewtemplate.dto.RegisterUser
import org.example.interviewtemplate.services.AuthService
import org.example.interviewtemplate.util.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.*
import java.net.URI

@Configuration
class AuthRouter(private val authHandler: AuthHandler) {

    @Bean
    fun authRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("api/auth/login", authHandler::login)
            POST("api/auth/register", authHandler::register)
        }
    }
}

@Service
class AuthHandler(private val service: AuthService) {
    private val logger = logger()

    suspend fun register(request: ServerRequest): ServerResponse {
        logger.info("request: api/auth/register")
        val body = request.awaitReceive<RegisterUser>()
        val newUser = service.register(body)
        return ServerResponse
            .created(URI.create("api/users/${newUser.name}"))
            .bodyValueAndAwait(newUser)
    }

    suspend fun login(request: ServerRequest): ServerResponse {
        logger.info("request: api/login/users")
        val body = request.awaitReceive<LoginUser>()
        val user = service.login(body)
            ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(user)
    }
}