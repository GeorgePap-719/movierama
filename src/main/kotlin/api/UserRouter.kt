package org.example.interviewtemplate.api

import org.example.interviewtemplate.dto.RegisterUser
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
            POST("api/users", userHandler::register)
            GET("api/users/{id}", userHandler::findById)
        }
    }
}

@Component
class UserHandler(private val service: UserService) {
    private val logger = logger()

    suspend fun register(request: ServerRequest): ServerResponse {
        logger.info("request: api/users")
        val body = request.awaitReceive<RegisterUser>()
        val newUser = service.register(body)
        //TODO:
//        return ServerResponse.created(URI.create("api/users/${newUser.id}"))
        return ServerResponse.status(201).bodyValueAndAwait(newUser)
    }

    suspend fun findById(request: ServerRequest): ServerResponse {
        logger.info("request: api/users/id")
        val idAsString = request.pathVariableOrNull("id")
        requireNotNull(idAsString) { "Target id is missing from path." }
        val id = idAsString.toIntOrNull()
            ?: throw IllegalArgumentException("Id must be a positive int, but got:$idAsString.")
        val user = service.findById(id) ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(user)
    }
}