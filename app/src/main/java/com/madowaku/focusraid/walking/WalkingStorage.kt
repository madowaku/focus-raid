package com.madowaku.focusraid.walking

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.walkingStore by preferencesDataStore(name = "walking_expedition")

class WalkingStorage(context: Context) {
    private val store = context.applicationContext.walkingStore
    private val key = stringPreferencesKey("journal_v1")
    suspend fun read(): WalkingJournal = decode(store.data.first()[key])
    suspend fun update(transform: (WalkingJournal) -> WalkingJournal): WalkingJournal {
        var result = WalkingJournal()
        store.edit { preferences ->
            result = transform(decode(preferences[key]))
            preferences[key] = encode(result)
        }
        return result
    }

    private fun encode(journal: WalkingJournal): String = JSONObject().apply {
        put("enabled", journal.enabled)
        put("firstDay", journal.firstDay?.toString())
        put("zone", journal.zone)
        put("days", JSONObject().apply {
            journal.days.forEach { (date, day) ->
                put(date.toString(), JSONObject().apply {
                    put("steps", day.steps ?: JSONObject.NULL)
                    put("checkpoints", JSONArray(day.checkpoints.sorted()))
                })
            }
        })
    }.toString()

    // Do not erase an unreadable ledger: surface a storage error instead of issuing rewards again.
    private fun decode(raw: String?): WalkingJournal {
        if (raw == null) return WalkingJournal()
        val json = JSONObject(raw)
        val days = json.getJSONObject("days")
        return WalkingJournal(
            enabled = json.getBoolean("enabled"),
            firstDay = json.optString("firstDay").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            zone = json.getString("zone"),
            days = days.keys().asSequence().associate { date ->
                val day = days.getJSONObject(date)
                val checkpoints = day.getJSONArray("checkpoints")
                LocalDate.parse(date) to WalkingDay(
                    if (day.isNull("steps")) null else day.getLong("steps"),
                    (0 until checkpoints.length()).map { checkpoints.getInt(it) }.toSet(),
                )
            },
        )
    }
}
