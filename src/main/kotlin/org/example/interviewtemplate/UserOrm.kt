package org.example.interviewtemplate

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingle
import kotlin.contracts.ExperimentalContracts
import kotlin.math.log

suspend fun mapToUserEntity(spec: DatabaseClient.GenericExecuteSpec, input: UserEntity): UserEntity? {
    return buildUser {
        val fetchSpec = spec.map { row, _ ->
            id = row.getColumn<Int>("id")
            name = input.name
            lastName = input.lastName
            phone = input.phone
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