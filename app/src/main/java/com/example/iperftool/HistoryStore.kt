package com.example.iperftool

import android.content.Context
import org.json.JSONArray

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("iperf_history", Context.MODE_PRIVATE)
    private val key = "commands"
    private val maxSize = 20

    fun load(): List<String> {
        val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
        return List(arr.length()) { arr.getString(it) }
    }

    private fun save(list: List<String>) {
        prefs.edit().putString(key, JSONArray(list).toString()).apply()
    }

    fun add(cmd: String): List<String> {
        val c = cmd.trim()
        if (c.isEmpty()) return load()
        val list = (listOf(c) + load().filter { it != c }).take(maxSize)
        save(list)
        return list
    }

    fun remove(cmd: String): List<String> {
        val list = load().filter { it != cmd }
        save(list)
        return list
    }

    fun clear() = prefs.edit().remove(key).apply()
}
