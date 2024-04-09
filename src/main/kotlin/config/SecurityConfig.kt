package org.example.interviewtemplate.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig(
    // The hash to be used for generating and validating passwords.
    @Value("\${salt}") val salt: String
)