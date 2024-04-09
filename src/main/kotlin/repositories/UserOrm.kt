package org.example.interviewtemplate.repositories

import org.example.interviewtemplate.entities.UserEntity
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle

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
        fetchSpec.awaitSingle()
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