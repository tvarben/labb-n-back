package mobappdev.example.nback_cimpl

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import mobappdev.example.nback_cimpl.data.AppDatabase
import mobappdev.example.nback_cimpl.data.ScoreDao
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

private const val APP_PREFERENCES_NAME = "game_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = APP_PREFERENCES_NAME
)

/**
 * Custom Application class for dependency injection of repositories and database.
 */
class GameApplication : Application() {
    lateinit var userPreferencesRepository: UserPreferencesRepository
    lateinit var scoreDao: ScoreDao

    // Backwards-compatibility alias for skeleton code typo
    val userPreferencesRespository: UserPreferencesRepository
        get() = userPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
        scoreDao = AppDatabase.getDatabase(this).scoreDao()
    }
}
