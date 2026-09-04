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

    @Query("SELECT * FROM focus_sessions ORDER BY completedAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FocusSessionEntity>>
}

@Database(
    entities = [FocusSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FocusRaidDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        fun create(context: Context): FocusRaidDatabase = Room.databaseBuilder(
            context.applicationContext,
            FocusRaidDatabase::class.java,
            "focus_raid.db",
        ).build()
    }
}

class RoomSessionHistoryRepository(
    private val dao: FocusSessionDao,
) : SessionHistoryRepository {
    // Keep enough local history in memory to derive long streaks without widening the UI itself.
    // The Adventure Log still renders only the most recent entries.
    override val recentSessions: Flow<List<SessionHistoryEntry>> =
        dao.observeRecent(400).map { rows -> rows.map(FocusSessionEntity::toDomain) }

    override suspend fun record(entry: SessionHistoryEntry) {
        dao.insert(entry.toEntity())
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
