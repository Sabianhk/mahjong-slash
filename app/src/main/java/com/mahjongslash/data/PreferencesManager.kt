package com.mahjongslash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mahjong_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        private val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
        private val KEY_LEFT_HANDED = booleanPreferencesKey("left_handed")
        private val KEY_LAST_SCORE = intPreferencesKey("last_score")
        // Store top 10 scores as comma-separated string
        private val KEY_HIGH_SCORES = stringPreferencesKey("high_scores")
        private val KEY_HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SOUND] ?: true }
    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAPTICS] ?: true }
    val leftHanded: Flow<Boolean> = context.dataStore.data.map { it[KEY_LEFT_HANDED] ?: false }
    val lastScore: Flow<Int> = context.dataStore.data.map { it[KEY_LAST_SCORE] ?: 0 }

    val hasSeenTutorial: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_SEEN_TUTORIAL] ?: false }

    val highScores: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_HIGH_SCORES] ?: ""
        if (raw.isEmpty()) emptyList()
        else raw.split(",").mapNotNull { it.toIntOrNull() }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    suspend fun setLeftHanded(leftHanded: Boolean) {
        context.dataStore.edit { it[KEY_LEFT_HANDED] = leftHanded }
    }

    suspend fun setTutorialSeen() {
        context.dataStore.edit { it[KEY_HAS_SEEN_TUTORIAL] = true }
    }

    suspend fun saveScore(score: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SCORE] = score

            val existing = prefs[KEY_HIGH_SCORES]?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toMutableList() ?: mutableListOf()

            existing.add(score)
            existing.sortDescending()
            val top10 = existing.take(10)
            prefs[KEY_HIGH_SCORES] = top10.joinToString(",")
        }
    }
}
