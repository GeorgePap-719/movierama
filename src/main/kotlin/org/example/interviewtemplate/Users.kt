package org.example.interviewtemplate

import kotlinx.serialization.Serializable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.bind
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class UserHandler(private val service: UserService) {
    private val logger = logger()

    suspend fun register(request: ServerRequest): ServerResponse {
        logger.info("request: api/register")
        val body = request.awaitReceive<RegisterUser>()
        val newUser = service.register(body)
        return ServerResponse.ok().bodyValueAndAwait(newUser)
    }
}

@Serializable
data class RegisterUser(val name: String, val lastName: String, val phone: String) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank." }
        require(lastName.isNotBlank()) { "Last name cannot be blank." }
        require(phone.isNotBlank()) { "Phone cannot be blank." }
    }
}

@Serializable
data class User(val id: Int, val name: String, val lastName: String, val phone: String)

@Service
class UserService(private val userRepository: UserRepository) {

    suspend fun register(input: RegisterUser): User {
        val user = UserEntity(
            name = input.name,
            lastName = input.lastName,
            phone = input.phone
        )
        return userRepository.save(user).toUser()
    }

    private fun UserEntity.toUser(): User {
        return User(id, name, lastName, phone)
    }

    fun findById(id: Int): User? {
        TODO()
    }
}

@Repository
class UserRepository(private val template: R2dbcEntityTemplate) {
    private val logger = logger()

    suspend fun save(input: UserEntity): UserEntity {
        check(input.id == 0) { "All ids should be zero, they are auto-increased by db." }
        logger.debug { "Saving user:$input" }
        val spec = template.databaseClient.sql {
            //language=MySQL
            "INSERT INTO template.users (name, last_name, phone) values (?, ?, ?)"
        }
            .bind<String>(0, input.name)
            .bind<String>(1, input.lastName)
            .bind<String>(2, input.phone)
        // By default, the behavior of an insert/update statement in the database
        // does not return the inserted/updated rows.
        // It returns the number of inserted/updated rows.
        val specWithFilter = spec.filter {
            statement -> statement.returnGeneratedValues("id")
        }
        val newUser = mapToUserEntity(specWithFilter, input)
        return checkNotNull(newUser)
    }
}

@Serializable
data class UserEntity(val id: Int = 0, val name: String, val lastName: String, val phone: String)