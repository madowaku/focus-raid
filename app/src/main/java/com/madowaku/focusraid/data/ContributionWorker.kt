package com.madowaku.focusraid.data

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class ContributionWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val database = FocusRaidDatabase.create(applicationContext)
        return try {
            val repository = FirebaseWorldRepository.createOrNull(applicationContext) ?: return Result.retry()
            if (ContributionDelivery(database.contributionDao(), repository).drain()) Result.retry() else Result.success()
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { Result.retry()
        } finally { database.close() }
    }

    companion object {
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            // Periodic recovery closes the gap between the durable DB commit and enqueue.
            manager.enqueueUniquePeriodicWork("world-contribution-recovery", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ContributionWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints).build())
            // APPEND_OR_REPLACE preserves a wake-up arriving while a previous drain finishes.
            manager.enqueueUniqueWork("world-contribution-delivery", ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<ContributionWorker>().setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
        }
    }
}
