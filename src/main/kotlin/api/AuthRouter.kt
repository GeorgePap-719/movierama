package org.example.interviewtemplate.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class AuthRouter(private val authHandler: AuthHandler) {

    @Bean
    fun authRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("api/users/login", authHandler::login)
            //TODO: add explanation why `Post`
            // see: https://stackoverflow.com/questions/3521290/logout-get-or-post/14587231#14587231
            // https://stackoverflow.com/questions/15098392/which-http-method-should-login-and-logout-actions-use-in-a-restful-setup
            POST("api/users/logout", authHandler::logout)
        }
    }
}

@Service
class AuthHandler {
    suspend fun login(request: ServerRequest): ServerResponse {
        TODO()
    }

    suspend fun logout(request: ServerRequest): ServerResponse {
        TODO()
    }
}