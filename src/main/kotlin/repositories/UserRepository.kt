package org.example.interviewtemplate.repositories

import kotlinx.coroutines.reactor.awaitSingle
import org.example.interviewtemplate.entities.UserEntity
import org.example.interviewtemplate.util.debug
import org.example.interviewtemplate.util.logger
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.r2dbc.core.bind
import org.springframework.stereotype.Repository

interface UserRepository {
    suspend fun save(input: UserEntity): UserEntity
    suspend fun findById(target: Int): UserEntity?
    suspend fun findByName(target: String): UserEntity?
    suspend fun deleteAll(): Int
}

@Repository
class UserRepositoryImpl(private val template: R2dbcEntityTemplate) : UserRepository {
    private val logger = logger()

    override suspend fun save(input: UserEntity): UserEntity {
        check(input.id == 0) { "All ids should be zero, they are auto-increased by db." }
        logger.debug { "Saving user:$input" }
        val spec = template.databaseClient.sql {
            //language=MySQL
            "INSERT INTO movierama.users (name, password) values (?, ?)"
        }
            .bind<String>(0, input.name)
            .bind<String>(1, input.encryptedPassword)
        val generatedId = saveAndReturnGeneratedId(spec)
        val newUser = input.copy(id = generatedId)
        return newUser
    }

    private suspend fun saveAndReturnGeneratedId(spec: DatabaseClient.GenericExecuteSpec): Int {
        // By default, the behavior of an insert/update statement in the database
        // does not return the inserted/updated rows.
        // It returns the number of inserted/updated rows.
        val specWithFilter = spec.filter { statement ->
            statement.returnGeneratedValues("id")
        }
        var id: Int? = null
        val fetchSpec = specWithFilter.map { row, _ ->
            id = row.getColumn<Int>("id")
        }
        fetchSpec.awaitSingle()
        return checkNotNull(id)
    }

    override suspend fun findById(target: Int): UserEntity? {
        val spec = template.databaseClient.sql {
            //language=MySQL
            "SELECT * FROM movierama.users where id=$target"
        }
        return mapToUserEntity(spec)
    }

    override suspend fun findByName(target: String): UserEntity? {
        val spec = template.databaseClient.sql {
            //language=MySQL
            "SELECT * FROM movierama.users where name='$target'"
        }
        return mapToUserEntity(spec)
    }

    // Mostly for helping in testing.
    override suspend fun deleteAll(): Int {
        logger.debug { "Deleting all users." }
        val spec = template.databaseClient.sql {
            //language=MySQL
            "DELETE FROM movierama.users"
        }
        return spec.fetch().rowsUpdated().awaitSingle().toInt()
    }
}