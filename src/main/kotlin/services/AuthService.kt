package org.example.interviewtemplate.services

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.example.interviewtemplate.dto.LoggedUser
import org.example.interviewtemplate.dto.User
import org.example.interviewtemplate.repositories.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.ParseException
import java.util.*

interface AuthService {
    suspend fun login(input: User): LoggedUser?
    suspend fun logout(user: LoggedUser)
    fun authorize(user: LoggedUser)
}

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    @Value("\${sharedkey}")
    private val _sharedKey: String
) : AuthService {
    private val sharedKey = _sharedKey.toByteArray()

    override suspend fun login(input: User): LoggedUser? {
        //TODO: assert password matches.
        val user = userRepository.findByName(input.name) ?: return null
        val token = createJwt(sharedKey)
        return LoggedUser(name = user.name, token = token)
    }

    override suspend fun logout(user: LoggedUser) {
        TODO("Not yet implemented")
    }

    override fun authorize(user: LoggedUser) {
        tryParseSignedJwt(user.token)
    }

    private fun tryParseSignedJwt(token: String) {
        val jwt = try {
            SignedJWT.parse(token)
        } catch (e: ParseException) {
            throw AuthorizationException()
        }
        val verifier = MACVerifier(sharedKey)
        try {
            jwt.verify(verifier)
        } catch (e: JOSEException) {
            throw AuthorizationException()
        } catch (e: IllegalStateException) {
            throw AuthorizationException()
        }
        if (jwt.jwtClaimsSet.issuer != issuer) throw AuthorizationException("Invalid issuer.")
        if (Date().before(jwt.jwtClaimsSet.expirationTime)) throw AuthorizationException("Session expired")
    }

    private fun createJwt(secret: ByteArray): String {
        // Create HMAC signer.
        val signer = MACSigner(secret)
        // Prepare JWT with claims set.
        val claimsSet = JWTClaimsSet.Builder()
            .issuer(issuer)
            .expirationTime(Date(Date().time + 60 * 1000))
            .build()
        val signedJwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsSet)
        // Apply the HMAC protection
        signedJwt.sign(signer)
        // Serialize to compact form, produces something like
        // eyJhbGciOiJIUzI1NiJ9.SGVsbG8sIHdvcmxkIQ.onO9Ihudz3WkiauDO2Uhyuz0Y18UASXlSc1eS0NkWyA
        return signedJwt.serialize()
    }

    private val issuer = "workable-interview"

}

class AuthorizationException(message: String? = null) : Exception(message)

// -------------------------------- JwtUtils --------------------------------