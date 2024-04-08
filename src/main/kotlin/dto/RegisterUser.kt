package org.example.interviewtemplate.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterUser(val name: String, val lastName: String, val phone: String) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank." }
        require(lastName.isNotBlank()) { "Last name cannot be blank." }
        require(phone.isNotBlank()) { "Phone cannot be blank." }
    }
}