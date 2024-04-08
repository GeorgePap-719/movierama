package org.example.interviewtemplate.dto

import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Int, val name: String, val lastName: String, val phone: String)