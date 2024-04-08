package org.example.interviewtemplate.dto

import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String, val password: String)