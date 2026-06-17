package com.bruce.controller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "terminal_history")

class TerminalHistoryManager(private val context: Context) {
    private val HISTORY_KEY = stringPreferencesKey("history_json")

    val history: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[HISTORY_KEY] ?: "[]"
        try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addCommand(command: String) {
        if (command.isBlank()) return
        context.dataStore.edit { preferences ->
            val jsonString = preferences[HISTORY_KEY] ?: "[]"
            val jsonArray = try {
                JSONArray(jsonString)
            } catch (e: Exception) {
                JSONArray()
            }

            // Удаляем старую такую же команду, чтобы она перешла в конец истории
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getString(i)
                if (item != command) {
                    list.add(item)
                }
            }
            list.add(command)

            // Ограничиваем историю 100 командами
            val trimmedList = if (list.size > 100) list.takeLast(100) else list

            val newJsonArray = JSONArray()
            trimmedList.forEach { newJsonArray.put(it) }

            preferences[HISTORY_KEY] = newJsonArray.toString()
        }
    }
}
