package org.example.interviewtemplate.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

// When needing to instantiate a logger for a class, do this:
// private val logger = logger()
inline fun <reified T> T.logger(): Logger =
    if (T::class.isCompanion)
        LoggerFactory.getLogger(T::class.java.enclosingClass)
    else
        LoggerFactory.getLogger(T::class.java)

// To write to metric log do this:
// logger.metric("message")
fun Logger.metric(msg: String) {
    this.info(MarkerFactory.getMarker("METRIC"), msg)
}

// -------------------------------- lazy-factories --------------------------------

fun Logger.debug(message: () -> String) {
    if (isDebugEnabled) debug(message())
}