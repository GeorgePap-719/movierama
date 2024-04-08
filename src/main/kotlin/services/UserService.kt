package org.example.interviewtemplate.services

import org.example.interviewtemplate.config.SecurityConfig
import org.example.interviewtemplate.dto.RegisterUser
import org.example.interviewtemplate.dto.User
import org.example.interviewtemplate.entities.UserEntity
import org.example.interviewtemplate.repositories.UserRepository
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UserService {
    suspend fun register(input: RegisterUser): User
    suspend fun findById(target: Int): User?
}

@Service
class UserServiceImpl(
    private val securityConfig: SecurityConfig,
    private val userRepository: UserRepository
) : UserService {

    @Transactional
    override suspend fun register(input: RegisterUser): User {
        val salt = securityConfig.salt
        val encryptedPassword = BCrypt.hashpw(input.password, salt)
        val user = UserEntity(
            name = input.name,
            encryptedPassword = encryptedPassword,
        )
        return userRepository.save(user).toUser()
    }

    private fun UserEntity.toUser(): User {
        return User(name, encryptedPassword)
    }

    override suspend fun findById(target: Int): User? {
        require(target > 0) { "Ids can only be positive, but got:$target." }
        return userRepository.findById(target)?.toUser()
    }
}