package mobappdev.example.nback_cimpl.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Score record entity for storing completed N-back round results.
 *
 * @property id Auto-incrementing primary key.
 * @property playerName Name of the player who completed the round.
 * @property score Final calculated non-negative score.
 * @property hits Number of successful match button presses.
 * @property misses Number of missed matches.
 * @property falseAlarms Number of presses when no match occurred.
 * @property nBack The 'n' distance used during the round.
 * @property gameType Visual, Audio, or AudioVisual (Dual).
 * @property timestamp Unix epoch millisecond timestamp of round completion.
 */

//22 allows for sorting and query access, also more convenient for persistency
@Entity(tableName = "scores")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val score: Int,
    val hits: Int,
    val misses: Int,
    val falseAlarms: Int,
    val nBack: Int,
    val gameType: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Room Data Access Object (DAO) for scores.
 *
 * Provides reactive Flows with SQLite @Query annotations that return
 * results sorted by score, player name, or date as required by the lab specification.
 *
 *
 *--  FLow<list> RETURNS FRESHLY SORTED LIST EVERY TIME THE DATABASE IS UPDATED.
 */
@Dao
interface ScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreRecord): Long

    @Query("SELECT * FROM scores ORDER BY score DESC, timestamp DESC")
    fun getScoresSortedByScoreDesc(): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM scores ORDER BY playerName COLLATE NOCASE ASC, score DESC")
    fun getScoresSortedByNameAsc(): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM scores ORDER BY timestamp DESC")
    fun getScoresSortedByDateDesc(): Flow<List<ScoreRecord>>

    @Query("SELECT MAX(score) FROM scores")
    fun getHighestScore(): Flow<Int?> //if empty return null
}

/**
 * Room Database singleton holder for score persistence.
 * Room auto-generates the concrete implementation of this abstract function at compile time, giving your app access to your @Dao interface methods.
 */
@Database(entities = [ScoreRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    //abstract function to get the DAOs
    abstract fun scoreDao(): ScoreDao

    //companion object is like a static block in java, ensures that only one instance of the database exists
    companion object {

        //make INSTANCE volatile so that all threads see the same instance. Avoids creating multiple instances of the database
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) { //return instance if it exists, otherwise lock thread and create instance
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nback_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
