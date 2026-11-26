package com.example.moneymate.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        @Volatile
        private var instance: UserManager? = null

        fun getInstance(context: Context): UserManager {
            return instance ?: synchronized(this) {
                instance ?: UserManager(context.applicationContext).also { instance = it }
            }
        }
    }

    data class User(
        val id: String,
        val name: String,
        val email: String,
        val password: String
    )

    fun hasCompletedOnboarding(): Boolean =
        sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun registerUser(name: String, email: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (getUserByEmail(normalizedEmail) != null) {
            return false
        }

        val user = User(
            id = System.currentTimeMillis().toString(),
            name = name.trim(),
            email = normalizedEmail,
            password = password
        )

        val users = getAllUsers().toMutableList()
        users.add(user)
        saveUsers(users)
        saveCurrentUser(user)
        return true
    }

    fun loginUser(email: String, password: String): User? {
        val normalizedEmail = email.trim().lowercase()
        val user = getUserByEmail(normalizedEmail)
        return if (user?.password == password) {
            saveCurrentUser(user)
            user
        } else {
            null
        }
    }

    fun loginDemoUser(): User {
        val demoEmail = "demo@moneymate.app"
        val existing = getUserByEmail(demoEmail)
        val demoUser = existing ?: User(
            id = "demo-user",
            name = "Alex Morgan",
            email = demoEmail,
            password = "demo123"
        ).also { created ->
            val users = getAllUsers().toMutableList()
            users.add(created)
            saveUsers(users)
        }
        setOnboardingCompleted(true)
        saveCurrentUser(demoUser)
        return demoUser
    }

    fun resetPassword(email: String, newPassword: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val users = getAllUsers().toMutableList()
        val index = users.indexOfFirst { it.email.equals(normalizedEmail, ignoreCase = true) }
        if (index == -1) return false

        val updated = users[index].copy(password = newPassword)
        users[index] = updated
        saveUsers(users)
        if (getCurrentUser()?.id == updated.id) {
            saveCurrentUser(updated)
        }
        return true
    }

    fun saveCurrentUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_CURRENT_USER, userJson).apply()
    }

    fun getCurrentUser(): User? {
        val userJson = sharedPreferences.getString(KEY_CURRENT_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            null
        }
    }

    fun logoutUser() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER).apply()
    }

    private fun getUserByEmail(email: String): User? {
        return getAllUsers().find { it.email.equals(email.trim(), ignoreCase = true) }
    }

    private fun getAllUsers(): List<User> {
        val usersJson = sharedPreferences.getString(KEY_USERS, "[]")
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson<List<User>>(usersJson, type) ?: emptyList()
    }

    private fun saveUsers(users: List<User>) {
        val usersJson = gson.toJson(users)
        sharedPreferences.edit().putString(KEY_USERS, usersJson).apply()
    }
}
