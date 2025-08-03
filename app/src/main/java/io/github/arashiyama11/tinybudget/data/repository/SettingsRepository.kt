package io.github.arashiyama11.tinybudget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val AMOUNT_STEP = longPreferencesKey("amount_step")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        private val DEFAULT_CATEGORY_ID = intPreferencesKey("default_category_id")
        private val LAST_CATEGORY_ID = intPreferencesKey("last_category_id")

        private val OVERLAY_X = floatPreferencesKey("overlay_x")
        private val OVERLAY_Y = floatPreferencesKey("overlay_y")
        private val OVERLAY_WIDTH = floatPreferencesKey("overlay_width")
        private val OVERLAY_HEIGHT = floatPreferencesKey("overlay_height")
        private val OVERLAY_DESTROYED_AT = longPreferencesKey("overlay_destroyed_at")

        private val TRIGGER_APPS = stringSetPreferencesKey("trigger_apps")

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
                preferences[OVERLAY_X] ?: DEFAULT_OVERLAY_POS_X,
                preferences[OVERLAY_Y] ?: DEFAULT_OVERLAY_POS_Y
            )
        }

    val overlaySize: Flow<Pair<Float, Float>> = dataStore.data
        .map { preferences ->
            Pair(
                preferences[OVERLAY_WIDTH] ?: DEFAULT_OVERLAY_SIZE_X,
                preferences[OVERLAY_HEIGHT] ?: DEFAULT_OVERLAY_SIZE_Y
            )
        }

    val overlayDestroyedAt: Flow<Long?> = dataStore.data
        .map { preferences -> preferences[OVERLAY_DESTROYED_AT] }

    val triggerApps: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[TRIGGER_APPS] ?: emptySet() }

    val amountStep: Flow<Long> = dataStore.data
        .map { preferences -> preferences[AMOUNT_STEP] ?: 10L }

    val sensitivity: Flow<Float> =
        dataStore.data
            .map { preferences -> preferences[SENSITIVITY] ?: 1f }

    suspend fun setDefaultCategoryId(id: Int) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_CATEGORY_ID] = id
        }
    }

    suspend fun setLastCategoryId(id: Int) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[LAST_CATEGORY_ID] = id
        }
    }

    suspend fun setOverlayPosition(x: Float, y: Float) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[OVERLAY_X] = x
            preferences[OVERLAY_Y] = y
        }
    }

    suspend fun setOverlaySize(width: Float, height: Float) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[OVERLAY_WIDTH] = width
            preferences[OVERLAY_HEIGHT] = height
        }
    }

    suspend fun setOverlayDestroyedAt(time: Long) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[OVERLAY_DESTROYED_AT] = time
        }
    }

    suspend fun resetOverlayPositionAndSize() = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences.remove(OVERLAY_X)
            preferences.remove(OVERLAY_Y)
            preferences.remove(OVERLAY_WIDTH)
            preferences.remove(OVERLAY_HEIGHT)
        }
    }

    suspend fun setTriggerApps(packageNames: Set<String>) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[TRIGGER_APPS] = packageNames
        }
    }

    suspend fun setAmountStep(step: Long) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[AMOUNT_STEP] = step
        }
    }

    suspend fun setSensitivity(multiplier: Float) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[SENSITIVITY] = multiplier
        }
    }
}
