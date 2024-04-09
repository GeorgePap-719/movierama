package org.example.interviewtemplate

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import org.springframework.web.reactive.function.client.toEntity

suspend inline fun <reified T : Any> RequestHeadersSpec<out RequestHeadersSpec<*>>.awaitRetrieveEntity(): ResponseEntity<T> {
    return retrieve().toEntity<T>().awaitSingle()
}