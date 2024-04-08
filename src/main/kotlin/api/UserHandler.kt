package org.example.interviewtemplate.api

import org.example.interviewtemplate.dto.RegisterUser
import org.example.interviewtemplate.services.UserService
import org.example.interviewtemplate.util.logger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI

@Component
class UserHandler(private val service: UserService) {
    private val logger = logger()

    suspend fun register(request: ServerRequest): ServerResponse {
        logger.info("request: api/users")
        val body = request.awaitReceive<RegisterUser>()
        val newUser = service.register(body)
        return ServerResponse.created(URI.create("api/users/${newUser.id}"))
            .bodyValueAndAwait(newUser)
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