package com.madowaku.focusraid.data

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
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Rarity
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val sessionId: String,
    val completedAtEpochMillis: Long,
    val plannedMinutes: Int,
    val creditedMinutes: Int,
    val expedition: String,
    val outcome: String,
    val damage: Int,
    val rarity: String?,
    val discovery: String?,
)

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContribution(row: WorldContribution)

    @Transaction
    suspend fun recordWithContribution(session: FocusSessionEntity, contribution: WorldContribution?) {
        insert(session)
        if (contribution != null) insertContribution(contribution)
    }

    @Query("SELECT * FROM focus_sessions ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>
}

@Database(
    entities = [FocusSessionEntity::class, WorldContribution::class],
    version = 2,
    exportSchema = false,
)
abstract class FocusRaidDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun contributionDao(): ContributionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS world_contributions (sessionId TEXT NOT NULL PRIMARY KEY, generation TEXT, creditedMinutes INTEGER NOT NULL, plannedMinutes INTEGER NOT NULL, completedAtEpochMillis INTEGER NOT NULL, status TEXT NOT NULL, appliedDamage INTEGER NOT NULL, startedAtEpochMillis INTEGER NOT NULL)")
            }
        }
        fun create(context: Context): FocusRaidDatabase = Room.databaseBuilder(
            context.applicationContext,
            FocusRaidDatabase::class.java,
            "focus_raid.db",
        ).addMigrations(MIGRATION_1_2).build()
    }
}

class RoomSessionHistoryRepository(
    private val dao: FocusSessionDao,
    private val onRecorded: () -> Unit = {},
) : SessionHistoryRepository {
    // Free and Pro persist the same complete local history. Visibility is a UI access policy only.
    override val recentSessions: Flow<List<SessionHistoryEntry>> =
        dao.observeAll().map { rows -> rows.map(FocusSessionEntity::toDomain) }

    override suspend fun record(entry: SessionHistoryEntry) {
        val contribution = if (entry.outcome == SessionOutcome.COMPLETED && entry.creditedMinutes > 0) {
            WorldContribution(entry.sessionId, entry.raidGeneration, entry.creditedMinutes, entry.plannedMinutes,
                entry.completedAtEpochMillis, if (entry.startedAtEpochMillis == 0L) "UNAVAILABLE" else "PENDING",
                startedAtEpochMillis = entry.startedAtEpochMillis)
        } else null
        dao.recordWithContribution(entry.toEntity(), contribution)
        // Scheduling failure must not turn a saved local reward into an error. Startup/periodic recovery retries.
        runCatching(onRecorded)
    }
}

private fun SessionHistoryEntry.toEntity(): FocusSessionEntity = FocusSessionEntity(
    sessionId = sessionId,
    completedAtEpochMillis = completedAtEpochMillis,
    plannedMinutes = plannedMinutes,
    creditedMinutes = creditedMinutes,
    expedition = expedition.name,
    outcome = outcome.name,
    damage = damage,
    rarity = rarity?.name,
    discovery = discovery,
)

private fun FocusSessionEntity.toDomain(): SessionHistoryEntry = SessionHistoryEntry(
    sessionId = sessionId,
    completedAtEpochMillis = completedAtEpochMillis,
    plannedMinutes = plannedMinutes,
    creditedMinutes = creditedMinutes,
    expedition = runCatching { Expedition.valueOf(expedition) }.getOrDefault(Expedition.TOWER),
    outcome = runCatching { SessionOutcome.valueOf(outcome) }.getOrDefault(SessionOutcome.COMPLETED),
    damage = damage,
    rarity = rarity?.let { runCatching { Rarity.valueOf(it) }.getOrNull() },
    discovery = discovery,
)
