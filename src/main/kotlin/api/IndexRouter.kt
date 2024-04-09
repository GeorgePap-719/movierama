package org.example.interviewtemplate.api

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class IndexRouter {
    fun index() = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            GET("/") {
                ServerResponse.ok().bodyValueAndAwait("index")
            }
            GET("/login") {
                ServerResponse.ok().bodyValueAndAwait("login")
            }
        }
    }
}