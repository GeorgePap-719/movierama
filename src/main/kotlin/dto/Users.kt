package org.example.interviewtemplate.dto

import kotlinx.serialization.Serializable
import org.example.interviewtemplate.entities.UserEntity

@Serializable
data class User(val name: String, val id: Int)

fun UserEntity.toUser(): User = User(name, id)

@Serializable
data class RegisterUser(val name: String, val password: String) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank." }
        require(password.isNotBlank()) { "Password cannot be blank." }
    }
}

@Serializable
data class LoginUser(val name: String, val password: String)

@Serializable
data class LoggedUser(val name: String, val token: String, val id: Int)

data class AuthenticatedUser(val name: String, val id: Int)