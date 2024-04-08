package org.example.interviewtemplate.api

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.bodyToMono
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

/**
 * Returns the path variable with the given name, or `null` if it is not present.
 */
fun ServerRequest.pathVariableOrNull(name: String): String? {
    val vars = pathVariables()
    return vars[name]
}

/**
 * Receives the incoming content for this [request][ServerRequest] and transforms it to the requested `T` type.
 *
 * @throws IllegalArgumentException when content cannot be transformed to the requested type.
 */
suspend inline fun <reified T : Any> ServerRequest.awaitReceive(): T = awaitReceiveNullable<T>()
    ?: throw ContentTransformationException(starProjectedType<T>())

/**
 * Receives the incoming content for this [request][ServerRequest] and transforms it to the requested `T` type.
 *
 * @throws IllegalArgumentException when content cannot be transformed to the requested type.
 */
suspend inline fun <reified T : Any> ServerRequest.awaitReceiveNullable(): T? {
    try {
        return bodyToMono<T>().awaitSingleOrNull()
    } catch (e: IllegalArgumentException) {
        throw ContentTransformationException(starProjectedType<T>())
    }
}

class ContentTransformationException(
    type: KType
) : IllegalArgumentException("Cannot transform this request's content to $type")

inline fun <reified T : Any> starProjectedType(): KType {
    return T::class.starProjectedType
}