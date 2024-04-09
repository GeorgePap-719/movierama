package org.example.interviewtemplate.util

import com.nimbusds.jwt.SignedJWT
import org.example.interviewtemplate.services.AuthenticationException
import java.text.ParseException

fun tryParseJwt(token: String): SignedJWT {
    return try {
        SignedJWT.parse(token)
    } catch (e: ParseException) {
        throw AuthenticationException()
    }
}