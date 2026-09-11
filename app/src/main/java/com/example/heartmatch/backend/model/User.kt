package com.example.heartmatch.backend.model

import com.example.heartmatch.engine.loader.JsonObject

data class User(
    val id: String,
    val username: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "username" to username,
        "createdAt" to createdAt,
        "lastLogin" to lastLogin
    )

    companion object {
        fun fromJsonObject(obj: JsonObject): User {
            val id = obj.getString("id") ?: ""
            val username = obj.getString("username") ?: "Player"
            val createdAt = obj.getLong("createdAt") ?: System.currentTimeMillis()
            val lastLogin = obj.getLong("lastLogin") ?: System.currentTimeMillis()
            return User(id = id, username = username, createdAt = createdAt, lastLogin = lastLogin)
        }
    }
}
