package org.example.interviewtemplate.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterUser(val name: String, val password: String) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank." }
        require(password.isNotBlank()) { "Password cannot be blank." }
    }
}