package com.example.heartmatch.backend.repository

import com.example.heartmatch.backend.model.User
import java.util.concurrent.ConcurrentHashMap

class UserRepository {
    private val users = ConcurrentHashMap<String, User>()

    fun getOrCreateUser(userId: String, username: String = "Player"): User {
        return users.compute(userId) { _, existing ->
            if (existing != null) {
                existing.copy(lastLogin = System.currentTimeMillis())
            } else {
                User(
                    id = userId,
                    username = username,
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis()
                )
            }
        }!!
    }

    fun findById(userId: String): User? = users[userId]

    fun save(user: User): User {
        users[user.id] = user
        return user
    }

    fun getAllUsers(): List<User> = users.values.toList()

    fun clear() {
        users.clear()
    }
}
