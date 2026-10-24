package com.example.data.repository

import android.util.Log

enum class OperationType {
    GET, LIST, CREATE, UPDATE, DELETE
}

object FirestoreErrorHandler {
    fun handleError(e: Exception, operation: OperationType, path: String) {
        val jsonMsg = """
            {
              "error": "${e.javaClass.simpleName}",
              "message": "${e.message?.replace("\"", "\\\"") ?: "Unknown error"}",
              "operation": "${operation.name}",
              "path": "$path"
            }
        """.trimIndent()
        Log.e("FirestoreError", jsonMsg, e)
    }
}
