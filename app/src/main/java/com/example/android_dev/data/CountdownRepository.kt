package com.example.android_dev.data

import android.content.Context
import com.example.android_dev.domain.Countdown
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class CountdownRepository(context: Context, username: String) {
    private val prefs = context.applicationContext
        .getSharedPreferences("smart_todo_countdown_${username.ifBlank { "_guest" }}", Context.MODE_PRIVATE)

    fun loadAll(): List<Countdown> {
        val raw = prefs.getString(KEY_COUNTDOWNS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toCountdown() }
        }.getOrDefault(emptyList())
    }

    fun saveAll(countdowns: List<Countdown>) {
        val array = JSONArray()
        countdowns.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_COUNTDOWNS, array.toString()).apply()
    }

    private fun Countdown.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("note", note)
            .put("targetDate", targetDate.toString())
            .put("createdAt", createdAt)
    }

    private fun JSONObject.toCountdown(): Countdown {
        return Countdown(
            id = optString("id"),
            title = optString("title"),
            note = optString("note"),
            targetDate = LocalDate.parse(optString("targetDate")),
            createdAt = optLong("createdAt", System.currentTimeMillis())
        )
    }

    companion object {
        private const val KEY_COUNTDOWNS = "countdowns"
    }
}