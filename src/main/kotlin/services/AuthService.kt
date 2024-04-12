package org.example.interviewtemplate.services

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.example.interviewtemplate.config.SecurityConfig
import org.example.interviewtemplate.dto.*
import org.example.interviewtemplate.entities.UserEntity
import org.example.interviewtemplate.repositories.UserRepository
import org.example.interviewtemplate.util.tryParseJwt
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.crypto.bcrypt.BCrypt.checkpw
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface AuthService {
    suspend fun register(input: RegisterUser): User
    suspend fun login(input: LoginUser): LoggedUser?
    fun authorize(token: String): String
}

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    @Value("\${sharedkey}")
    private val _sharedKey: String,
    private val securityConfig: SecurityConfig
) : AuthService {
    private val sharedKey = _sharedKey.toByteArray()

    @Transactional
    override suspend fun register(input: RegisterUser): User {
        val salt = securityConfig.salt
        val encryptedPassword = BCrypt.hashpw(input.password, salt)
        val user = UserEntity(
            name = input.name,
            encryptedPassword = encryptedPassword,
        )
        return trySave(user).toUser()
    }

    private suspend fun trySave(input: UserEntity): UserEntity {
        try {
            return userRepository.save(input)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException("The user with name:${input.name} already exists.")
        }
    }

    override suspend fun login(input: LoginUser): LoggedUser? {
        val user = userRepository.findByName(input.name) ?: return null
        if (!checkpw(input.password, user.encryptedPassword)) throw AuthenticationException()
        val token = createJwt(sharedKey, user.name)
        return LoggedUser(name = user.name, token = token, id = user.id)
    }

    private fun createJwt(secret: ByteArray, username: String): String {
        // Create HMAC signer.
        val signer = MACSigner(secret)
        // Prepare JWT with claims set.
        val claimsSet = JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(username)
            // Make it valid for 1 hour.
            .expirationTime(Date(Date().time + 60 * 1000))
            .build()
        val signedJwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsSet)
        // Apply the HMAC protection.
        signedJwt.sign(signer)
        // Serialize to compact form, produces something like
        // eyJhbGciOiJIUzI1NiJ9.SGVsbG8sIHdvcmxkIQ.onO9Ihudz3WkiauDO2Uhyuz0Y18UASXlSc1eS0NkWyA
        return signedJwt.serialize()
    }

    override fun authorize(token: String): String {
        val jwt = tryParseJwt(token)
        val verifier = MACVerifier(sharedKey)
        try {
            if (!jwt.verify(verifier)) throw AuthenticationException("Signature failed verification.")
        } catch (e: JOSEException) {
            throw AuthenticationException()
        } catch (e: IllegalStateException) {
            throw AuthenticationException()
        }
        if (jwt.jwtClaimsSet.issuer != issuer) throw AuthenticationException("Invalid issuer.")
        if (Date().after(jwt.jwtClaimsSet.expirationTime)) throw AuthenticationException("Session expired.")
        return jwt.jwtClaimsSet.subject.toString()
    }

    private val issuer = "workable-interview"

}

class AuthenticationException(message: String? = null) : Exception(message)