package org.example.interviewtemplate.api

import org.example.interviewtemplate.api.utils.pathVariableOrNull
import org.example.interviewtemplate.services.UserService
import org.example.interviewtemplate.util.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Configuration
class UserRouter(private val userHandler: UserHandler) {

    @Bean
    fun userRoutes() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            GET("api/users/{name}", userHandler::findByName)
        }
    }
}

@Component
class UserHandler(private val service: UserService) {
    private val logger = logger()

    suspend fun findByName(request: ServerRequest): ServerResponse {
        logger.info("request: api/users/name")
        val name = request.pathVariableOrNull("name")
        requireNotNull(name) { "Target name is missing from path." }
        val user = service.findByName(name) ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(user)
    }
}