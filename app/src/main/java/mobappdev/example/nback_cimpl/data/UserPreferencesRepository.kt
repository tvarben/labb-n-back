package mobappdev.example.nback_cimpl.data


import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mobappdev.example.nback_cimpl.domain.GameSettings
import java.io.IOException

/**
 * Repository providing persistent key-value storage using Jetpack DataStore<Preferences>.
 *
 * Persists the user's highscore as well as persistent configuration settings:
 * - nBack (default: 2)
 * - eventCount (default: 20)
 * - eventDurationMs (default: 2000 ms)
 * - gridSize (default: 3 for 3x3)
 * - letterCount (default: 8 distinct spoken letters)
 * - targetMatchPercentage (default: 30%)
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    //exists in ram when application is running.
    private companion object {
        val KEY_HIGHSCORE = intPreferencesKey("highscore")
        val KEY_N_BACK = intPreferencesKey("n_back")
        val KEY_EVENT_COUNT = intPreferencesKey("event_count")
        val KEY_EVENT_DURATION = longPreferencesKey("event_duration_ms")
        val KEY_GRID_SIZE = intPreferencesKey("grid_size")
        val KEY_LETTER_COUNT = intPreferencesKey("letter_count")
        val KEY_MATCH_PERCENTAGE = intPreferencesKey("target_match_percentage")
        const val TAG = "UserPreferencesRepo" //for logging
    }

    //
    private val preferencesFlow: Flow<Preferences> = dataStore.data // load saved settings into
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception) //log the error
                emit(emptyPreferences()) //use default values instead
            } else {
                throw exception
            }
        }

    /**
     * Flow emitting the saved high score.
     */
    val highscore: Flow<Int> = preferencesFlow.map { prefs -> //get highscore from the preferences file
        prefs[KEY_HIGHSCORE] ?: 0 //if null set to 0
    }

    /**
     * Flow emitting the consolidated GameSettings.
     */
    val gameSettings: Flow<GameSettings> = preferencesFlow.map { prefs -> //map all gameSettings into one object
        GameSettings(
            nBack = prefs[KEY_N_BACK] ?: 2,
            eventCount = prefs[KEY_EVENT_COUNT] ?: 20,
            eventDurationMs = prefs[KEY_EVENT_DURATION] ?: 2000L,
            gridSize = prefs[KEY_GRID_SIZE] ?: 3,
            targetMatchPercentage = prefs[KEY_MATCH_PERCENTAGE] ?: 30,
            letterCount = prefs[KEY_LETTER_COUNT] ?: 8
        )
    }

    suspend fun saveHighScore(score: Int) { //check if new score is higher than current highscore from datastore file
        dataStore.edit { prefs ->
            val current = prefs[KEY_HIGHSCORE] ?: 0
            if (score > current) {
                prefs[KEY_HIGHSCORE] = score
            }
        }
    }

    suspend fun saveSettings(settings: GameSettings) { //save settings to datastore file on disk
        dataStore.edit { prefs ->
            prefs[KEY_N_BACK] = settings.nBack
            prefs[KEY_EVENT_COUNT] = settings.eventCount
            prefs[KEY_EVENT_DURATION] = settings.eventDurationMs
            prefs[KEY_GRID_SIZE] = settings.gridSize
            prefs[KEY_LETTER_COUNT] = settings.letterCount
            prefs[KEY_MATCH_PERCENTAGE] = settings.targetMatchPercentage
        }
    }
}
