package org.example.interviewtemplate.api

import org.example.interviewtemplate.dto.LoginUser
import org.example.interviewtemplate.services.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.*

@Configuration
class AuthRouter(private val authHandler: AuthHandler) {

    @Bean
    fun authRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("api/users/login", authHandler::login)
        }
    }
}

@Service
class AuthHandler(private val service: AuthService) {
    suspend fun login(request: ServerRequest): ServerResponse {
        val body = request.awaitReceive<LoginUser>()
        val user = service.login(body)
            ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(user)
    }
}