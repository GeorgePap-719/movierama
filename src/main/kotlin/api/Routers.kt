package org.example.interviewtemplate.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class Routers(private val userHandler: UserHandler) {

    @Bean
    fun userRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("api/users", userHandler::register)
            GET("api/users/{id}", userHandler::findById)
        }
    }
}