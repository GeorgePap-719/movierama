package org.example.interviewtemplate.repositories.util

import io.r2dbc.spi.Row
import org.springframework.data.r2dbc.mapping.OutboundRow
import org.springframework.r2dbc.core.Parameter.from
import org.springframework.r2dbc.core.Parameter.fromOrEmpty

inline fun <reified T> Row.getColumn(name: String): T =
    this[name, T::class.java] ?: error("There is a null value inside $name")

inline fun <reified T> Row.getColumnOrNull(name: String): T? = this[name, T::class.java]

fun <T : Any> OutboundRow.append(key: String, value: T): OutboundRow = this.append(key, from(value))

inline fun <reified T> OutboundRow.appendOrEmpty(key: String, value: T): OutboundRow =
    append(key, fromOrEmpty(value, T::class.java))

fun checkForSingleRowUpdate(rows: Long) {
    check(rows == 1L) {
        "Expected rows to be affected is one but updated:$rows."
    }
}

fun checkForDoubleRowUpdate(rows: Long) {
    check(rows == 2L) {
        "Expected rows to be affected is two but updated:$rows."
    }
}