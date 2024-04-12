package org.example.interviewtemplate.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun <T : Any> T?.toMono(): Mono<T> = Mono.justOrEmpty(this)

fun <T : Any> Flux<T?>.nullableAsFlow(): Flow<T> {
    // We need to cast it to non-nullable Flux<T>,
    // as .asFlow() function accepts only non-nullable `T`.
    val casted: Flux<T> = mapNotNull { it }
    return casted.asFlow()
}