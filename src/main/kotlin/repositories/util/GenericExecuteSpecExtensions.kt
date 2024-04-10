package org.example.interviewtemplate.repositories.util

import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import org.springframework.r2dbc.core.awaitSingle

suspend fun GenericExecuteSpec.saveAndReturnGeneratedId(): Int {
    // By default, the behavior of an insert/update statement in the database
    // does not return the inserted/updated rows.
    // It returns the number of inserted/updated rows.
    val specWithFilter = filter { statement ->
        statement.returnGeneratedValues("id")
    }
    var id: Int? = null
    val fetchSpec = specWithFilter.map { row, _ ->
        id = row.getColumn<Int>("id")
    }
    fetchSpec.awaitSingle()
    return checkNotNull(id)
}