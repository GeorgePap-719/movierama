package org.example.interviewtemplate

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle

suspend fun mapToUserEntity(spec: DatabaseClient.GenericExecuteSpec): UserEntity? {
    return buildUser {
        val fetchSpec = spec.map { row, metadata ->
            check(metadata.columnMetadatas.size == 4) {
                "Size should be equal of `UserEntity` fields, but got:${metadata.columnMetadatas.size}."
            }
            id = row.getColumn<Int>("id")
            name = row.getColumn<String>("name")
            lastName = row.getColumn<String>("last_name")
            phone = row.getColumn<String>("phone")
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
    var lastName: String? = null
    var phone: String? = null

    val isEmpty: Boolean get() = id == null && name == null && lastName == null && phone == null

    fun build(): UserEntity? {
        if (isEmpty) return null
        val id = requireNotNull(id)
        val name = requireNotNull(name)
        val lastName = requireNotNull(lastName)
        val phone = requireNotNull(phone)
        return UserEntity(
            id = id,
            name = name,
            lastName = lastName,
            phone = phone
        )
    }
}