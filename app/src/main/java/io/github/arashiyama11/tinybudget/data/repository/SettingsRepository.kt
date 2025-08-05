package io.github.arashiyama11.tinybudget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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


interface SettingsRepository {
    val defaultCategoryId: Flow<Int?>
    val lastCategoryId: Flow<Int?>
    val isLastModeNumeric: Flow<Boolean>
    val overlayPosition: Flow<Pair<Float, Float>>
    val overlaySize: Flow<Pair<Float, Float>>
    val overlayDestroyedAt: Flow<Long?>
    val triggerApps: Flow<Set<String>>
    val amountStep: Flow<Long>
    val sensitivity: Flow<Float>
    val frictionMultiplier: Flow<Float>

    suspend fun setDefaultCategoryId(id: Int)
    suspend fun setLastCategoryId(id: Int)
    suspend fun setLastModeNumeric(isNumeric: Boolean)
    suspend fun setOverlayPosition(x: Float, y: Float)
    suspend fun setOverlaySize(width: Float, height: Float)
    suspend fun setOverlayDestroyedAt(time: Long)
    suspend fun resetOverlayPositionAndSize()
    suspend fun setTriggerApps(packageNames: Set<String>)
    suspend fun setAmountStep(step: Long)
    suspend fun setSensitivity(multiplier: Float)
    suspend fun setFrictionMultiplier(multiplier: Float)
}

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    companion object {
        val AMOUNT_STEP = longPreferencesKey("amount_step")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val FRICTION_MULTIPLIER = floatPreferencesKey("friction_multiplier")
        private val DEFAULT_CATEGORY_ID = intPreferencesKey("default_category_id")
        private val LAST_CATEGORY_ID = intPreferencesKey("last_category_id")
        private val IS_LAST_MODE_NUMERIC = booleanPreferencesKey("is_last_mode_numeric")

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

    override val defaultCategoryId: Flow<Int?> = dataStore.data
        .map { preferences -> preferences[DEFAULT_CATEGORY_ID] }

    override val lastCategoryId: Flow<Int?> = dataStore.data
        .map { preferences -> preferences[LAST_CATEGORY_ID] }

    override val isLastModeNumeric: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_LAST_MODE_NUMERIC] ?: false }

    override val overlayPosition: Flow<Pair<Float, Float>> = dataStore.data
        .map { preferences ->
            Pair(
                preferences[OVERLAY_X] ?: DEFAULT_OVERLAY_POS_X,
                preferences[OVERLAY_Y] ?: DEFAULT_OVERLAY_POS_Y
            )
        }

    override val overlaySize: Flow<Pair<Float, Float>> = dataStore.data
        .map { preferences ->
            Pair(
                preferences[OVERLAY_WIDTH] ?: DEFAULT_OVERLAY_SIZE_X,
                preferences[OVERLAY_HEIGHT] ?: DEFAULT_OVERLAY_SIZE_Y
            )
        }

    override val overlayDestroyedAt: Flow<Long?> = dataStore.data
        .map { preferences -> preferences[OVERLAY_DESTROYED_AT] }

    override val triggerApps: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[TRIGGER_APPS] ?: emptySet() }

    override val amountStep: Flow<Long> = dataStore.data
        .map { preferences -> preferences[AMOUNT_STEP] ?: 10L }

    override val sensitivity: Flow<Float> =
        dataStore.data
            .map { preferences -> preferences[SENSITIVITY] ?: 1f }

    override val frictionMultiplier: Flow<Float> = dataStore.data
        .map { preferences -> preferences[FRICTION_MULTIPLIER] ?: 1f }

    override suspend fun setDefaultCategoryId(id: Int): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_CATEGORY_ID] = id
        }
    }

    override suspend fun setLastCategoryId(id: Int): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[LAST_CATEGORY_ID] = id
        }
    }

    override suspend fun setLastModeNumeric(isNumeric: Boolean): Unit =
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[IS_LAST_MODE_NUMERIC] = isNumeric
            }
        }

    override suspend fun setOverlayPosition(x: Float, y: Float): Unit =
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[OVERLAY_X] = x
                preferences[OVERLAY_Y] = y
            }
        }

    override suspend fun setOverlaySize(width: Float, height: Float): Unit =
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[OVERLAY_WIDTH] = width
                preferences[OVERLAY_HEIGHT] = height
            }
        }

    override suspend fun setOverlayDestroyedAt(time: Long): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[OVERLAY_DESTROYED_AT] = time
        }
    }

    override suspend fun resetOverlayPositionAndSize(): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences.remove(OVERLAY_X)
            preferences.remove(OVERLAY_Y)
            preferences.remove(OVERLAY_WIDTH)
            preferences.remove(OVERLAY_HEIGHT)
        }
    }

    override suspend fun setTriggerApps(packageNames: Set<String>): Unit =
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[TRIGGER_APPS] = packageNames
            }
        }

    override suspend fun setAmountStep(step: Long): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[AMOUNT_STEP] = step
        }
    }

    override suspend fun setSensitivity(multiplier: Float): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences[SENSITIVITY] = multiplier
        }
    }

    override suspend fun setFrictionMultiplier(multiplier: Float): Unit =
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[FRICTION_MULTIPLIER] = multiplier
            }
        }
}
