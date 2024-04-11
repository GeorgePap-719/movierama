package org.example.interviewtemplate.util

import reactor.core.publisher.Mono

fun <T : Any> T?.toMono(): Mono<T> = Mono.justOrEmpty(this)