package com.madowaku.focusraid.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout

enum class ContributionStatus { PENDING, RETRYING, OFFLINE, AUTH_REQUIRED, ACCEPTED, ALREADY_COUNTED, STALE, REJECTED, UNAVAILABLE }

@Entity(tableName = "world_contributions")
data class WorldContribution(
    @PrimaryKey val sessionId: String,
    val generation: String?,
    val creditedMinutes: Int,
    val plannedMinutes: Int,
    val completedAtEpochMillis: Long,
    val status: String = ContributionStatus.PENDING.name,
    val appliedDamage: Int = 0,
    val startedAtEpochMillis: Long = 0,
) {
    val state: ContributionStatus get() = ContributionStatus.valueOf(status)
}

@Dao
interface ContributionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: WorldContribution)

    @Query("SELECT * FROM world_contributions ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<WorldContribution>>

    @Query("SELECT * FROM world_contributions WHERE status IN ('PENDING','RETRYING','OFFLINE','AUTH_REQUIRED') ORDER BY completedAtEpochMillis LIMIT 50")
    suspend fun pending(): List<WorldContribution>

    @Query("UPDATE world_contributions SET status = :status, appliedDamage = :damage WHERE sessionId = :id AND status IN ('PENDING','RETRYING','OFFLINE','AUTH_REQUIRED')")
    suspend fun resolve(id: String, status: String, damage: Int = 0)
}

data class ContributionReceipt(val status: ContributionStatus, val appliedDamage: Int)
class ContributionAuthException : Exception()
class ContributionRejectedException : Exception()

fun interface ContributionGateway {
    suspend fun submit(row: WorldContribution): ContributionReceipt
}

/** Durable rows survive cancellation even between server commit and local acknowledgement. */
class ContributionDelivery(private val dao: ContributionDao, private val gateway: ContributionGateway) {
    suspend fun drain(): Boolean {
        var retry = false
        for (row in dao.pending()) {
            dao.resolve(row.sessionId, ContributionStatus.RETRYING.name)
            try {
                val receipt = withTimeout(15_000) { gateway.submit(row) }
                check(receipt.status in setOf(ContributionStatus.ACCEPTED, ContributionStatus.ALREADY_COUNTED,
                    ContributionStatus.STALE, ContributionStatus.REJECTED))
                check(receipt.appliedDamage in 0..row.creditedMinutes)
                dao.resolve(row.sessionId, receipt.status.name, receipt.appliedDamage)
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                dao.resolve(row.sessionId, ContributionStatus.OFFLINE.name); retry = true
            } catch (e: CancellationException) { throw e
            } catch (_: ContributionAuthException) {
                dao.resolve(row.sessionId, ContributionStatus.AUTH_REQUIRED.name); retry = true
            } catch (_: ContributionRejectedException) {
                dao.resolve(row.sessionId, ContributionStatus.REJECTED.name)
            } catch (_: Exception) {
                dao.resolve(row.sessionId, ContributionStatus.OFFLINE.name); retry = true
            }
        }
        return retry || dao.pending().isNotEmpty()
    }
}
