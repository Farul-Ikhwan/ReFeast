package com.refeast.app.util

import android.content.Context

/** Remembers who is logged in using SharedPreferences, so the app opens straight to Home next time. */
class Session(context: Context) {

    private val prefs = context.getSharedPreferences("refeast_session", Context.MODE_PRIVATE)

    fun login(userId: Long, role: String) {
        prefs.edit().putLong("user_id", userId).putString("role", role).apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun getUserId(): Long = prefs.getLong("user_id", -1)

    fun getRole(): String = prefs.getString("role", "") ?: ""

    fun isLoggedIn(): Boolean = getUserId() != -1L
}
