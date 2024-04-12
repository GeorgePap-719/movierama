package org.example.interviewtemplate.repositories.orm

import kotlinx.coroutines.flow.toList
import org.example.interviewtemplate.entities.UserEntity
import org.example.interviewtemplate.repositories.util.getColumn
import org.example.interviewtemplate.util.nullableAsFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingleOrNull

suspend fun mapToUserEntities(spec: DatabaseClient.GenericExecuteSpec): List<UserEntity> {
    val fetchSpec = spec.map { row, metadata ->
        buildUser {
            check(metadata.columnMetadatas.size == 3) {
                "Size should be equal of `UserEntity` fields, but got:${metadata.columnMetadatas.size}."
            }
            id = row.getColumn<Int>("id")
            name = row.getColumn<String>("name")
            password = row.getColumn<String>("password")
        }
    }
    return fetchSpec.all().nullableAsFlow().toList()
}

suspend fun mapToUserEntity(spec: DatabaseClient.GenericExecuteSpec): UserEntity? {
    return buildUser {
        val fetchSpec = spec.map { row, metadata ->
            check(metadata.columnMetadatas.size == 3) {
                "Size should be equal of `UserEntity` fields, but got:${metadata.columnMetadatas.size}."
            }
            id = row.getColumn<Int>("id")
            name = row.getColumn<String>("name")
            password = row.getColumn<String>("password")
        }
        fetchSpec.awaitSingleOrNull()
    }
}

private inline fun buildUser(action: UserBuilder.() -> Unit): UserEntity? {
    val builder = UserBuilder()
    builder.action()
    return builder.build()
}

private class UserBuilder {
    var id: Int? = null
    var name: String? = null
    var password: String? = null

    val isEmpty: Boolean get() = id == null && name == null && password == null

    fun build(): UserEntity? {
        if (isEmpty) return null
        val id = requireNotNull(id)
        val name = requireNotNull(name)
        val password = requireNotNull(password)
        return UserEntity(
            id = id,
            name = name,
            encryptedPassword = password,
        )
    }
}