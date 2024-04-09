package org.example.interviewtemplate

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.security.SecureRandom
import java.util.*
import kotlin.test.Test


class SimpleTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun test() {
        val random = SecureRandom()
        val sharedSecret = ByteArray(256)
        random.nextBytes(sharedSecret)
        println(sharedSecret.toHexString())
        // Create HMAC signer

        // Prepare JWT with claims set
        val claimsSet = JWTClaimsSet.Builder()
            .issuer("workable-interview")
            .expirationTime(Date(Date().time + 60 * 1000))
            .build()
        var signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsSet)
        // Apply the HMAC protection
//        signedJWT.sign(signer)
        // Serialize to compact form, produces something like
        // eyJhbGciOiJIUzI1NiJ9.SGVsbG8sIHdvcmxkIQ.onO9Ihudz3WkiauDO2Uhyuz0Y18UASXlSc1eS0NkWyA
        val s = signedJWT.serialize()
        // On the consumer side, parse the JWS and verify its HMAC
        signedJWT = SignedJWT.parse(s)
        val verifier: JWSVerifier = MACVerifier(sharedSecret)
        assertTrue(signedJWT.verify(verifier))
        // Retrieve / verify the JWT claims according to the app requirements
        assertEquals("alice", signedJWT.jwtClaimsSet.subject)
        assertEquals("https://c2id.com", signedJWT.jwtClaimsSet.issuer)
        assertTrue(Date().before(signedJWT.jwtClaimsSet.expirationTime))
    }
}