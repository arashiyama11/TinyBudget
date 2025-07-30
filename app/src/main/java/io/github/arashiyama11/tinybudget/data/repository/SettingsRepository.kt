package io.github.arashiyama11.tinybudget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val DEFAULT_CATEGORY_ID = intPreferencesKey("default_category_id")
        private val LAST_CATEGORY_ID = intPreferencesKey("last_category_id")

        private val OVERLAY_SIZE_X = floatPreferencesKey("overlay_size_x")
        private val OVERLAY_SIZE_Y = floatPreferencesKey("overlay_size_y")
        private val OVERLAY_POS_X = floatPreferencesKey("overlay_pos_x")
        private val OVERLAY_POS_Y = floatPreferencesKey("overlay_pos_y")

        private const val DEFAULT_OVERLAY_SIZE_X = 160f
        private const val DEFAULT_OVERLAY_SIZE_Y = 200f

        private const val DEFAULT_OVERLAY_POS_X = 0f
        private const val DEFAULT_OVERLAY_POS_Y = 0f
    }

    val defaultCategoryId: Flow<Int?> = dataStore.data
        .map { preferences -> preferences[DEFAULT_CATEGORY_ID] }

    val lastCategoryId: Flow<Int?> = dataStore.data
        .map { preferences -> preferences[LAST_CATEGORY_ID] }

    val overlayPosition: Flow<Pair<Float, Float>> = dataStore.data
        .map { preferences ->
            Pair(
                preferences[OVERLAY_POS_X] ?: DEFAULT_OVERLAY_POS_X,
                preferences[OVERLAY_POS_Y] ?: DEFAULT_OVERLAY_POS_Y
            )
        }

    val overlaySize: Flow<Pair<Float, Float>> = dataStore.data
        .map { preferences ->
            Pair(
                preferences[OVERLAY_SIZE_X] ?: DEFAULT_OVERLAY_SIZE_X,
                preferences[OVERLAY_SIZE_Y] ?: DEFAULT_OVERLAY_SIZE_Y
            )
        }

    suspend fun setDefaultCategoryId(id: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_CATEGORY_ID] = id
        }
    }

    suspend fun setLastCategoryId(id: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_CATEGORY_ID] = id
        }
    }

    suspend fun setOverlayPosition(x: Float, y: Float) {
        dataStore.edit { preferences ->
            preferences[OVERLAY_POS_X] = x
            preferences[OVERLAY_POS_Y] = y
        }
    }

    suspend fun setOverlaySize(width: Float, height: Float) {
        dataStore.edit { preferences ->
            preferences[OVERLAY_SIZE_X] = width
            preferences[OVERLAY_SIZE_Y] = height
        }
    }
}
