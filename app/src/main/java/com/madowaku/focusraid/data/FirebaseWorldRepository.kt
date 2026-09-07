package com.madowaku.focusraid.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.madowaku.focusraid.BuildConfig
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Footprint
import com.madowaku.focusraid.core.model.FootprintPresets
import com.madowaku.focusraid.core.model.WorldSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseWorldRepository private constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val fallback: WorldRepository,
) : WorldRepository {
    private val _world = MutableStateFlow(unavailableWorld())
    override val world: StateFlow<WorldSnapshot> = _world.asStateFlow()

    private val _syncStatus = MutableStateFlow(WorldSyncStatus.CONNECTING)
    override val syncStatus: StateFlow<WorldSyncStatus> = _syncStatus.asStateFlow()

    override suspend fun refresh() {
        _syncStatus.value = WorldSyncStatus.CONNECTING
        try {
            withTimeout(12_000) {
            ensureSignedIn()
            val document = firestore
                .collection(WORLD_COLLECTION)
                .document(CURRENT_WORLD_DOCUMENT)
                .get(Source.SERVER)
                .await()
            check(document.exists()) { "world/current does not exist" }
            _world.value = RemoteWorldMapping.fromMap(
                values = document.data.orEmpty(),
                fallback = unavailableWorld(),
            )
            _syncStatus.value = WorldSyncStatus.LIVE
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            _syncStatus.value = WorldSyncStatus.OFFLINE
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            // Retain the last server snapshot, never substitute invented participants.
            _syncStatus.value = WorldSyncStatus.OFFLINE
        }
    }

    override suspend fun footprints(
        expedition: Expedition,
        checkpoint: Int,
        limit: Int,
    ): List<Footprint> {
        if (limit <= 0) return emptyList()

        return withTimeout(12_000) {
            ensureSignedIn()
            footprintEntries(expedition, checkpoint)
                .orderBy(CREATED_AT_FIELD, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get(Source.SERVER)
                .await()
                .documents
                .mapNotNull { document ->
                    val presetId = document.getString(PRESET_ID_FIELD) ?: return@mapNotNull null
                    val preset = FootprintPresets.byId(presetId) ?: return@mapNotNull null
                    Footprint(
                        expedition = expedition,
                        checkpoint = checkpoint,
                        presetId = preset.id,
                        glyph = preset.glyph,
                        text = preset.text,
                        relativeLabel = relativeLabel(document.getTimestamp(CREATED_AT_FIELD)),
                    )
                }
        }
    }

    override suspend fun leaveFootprint(
        expedition: Expedition,
        checkpoint: Int,
        presetId: String,
    ): Footprint? {
        val preset = FootprintPresets.byId(presetId) ?: return null

        return withTimeout(12_000) {
            val userId = ensureSignedIn()
            val document = footprintEntries(expedition, checkpoint).document(userId)
            val existing = document.get(Source.SERVER).await()
            val existingCreatedAt = existing.getTimestamp(CREATED_AT_FIELD)

            if (existing.exists()) {
                // One user owns one footprint per location. Changing the preset does not bump recency.
                document.update(PRESET_ID_FIELD, preset.id).await()
            } else {
                document.set(
                    mapOf(
                        PRESET_ID_FIELD to preset.id,
                        CREATED_AT_FIELD to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }

            Footprint(
                expedition = expedition,
                checkpoint = checkpoint,
                presetId = preset.id,
                glyph = preset.glyph,
                text = preset.text,
                relativeLabel = relativeLabel(existingCreatedAt),
            )
        }
    }

    private suspend fun ensureSignedIn(): String {
        auth.currentUser?.uid?.let { return it }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Firebase anonymous sign-in returned no user")
    }

    private fun footprintEntries(
        expedition: Expedition,
        checkpoint: Int,
    ): CollectionReference = firestore
        .collection(FOOTPRINTS_COLLECTION)
        .document(expedition.name)
        .collection(CHECKPOINTS_COLLECTION)
        .document(checkpoint.coerceAtLeast(0).toString())
        .collection(ENTRIES_COLLECTION)

    private fun relativeLabel(timestamp: Timestamp?): String {
        val createdAtMillis = timestamp?.toDate()?.time ?: return "たった今"
        val elapsedMinutes = ((System.currentTimeMillis() - createdAtMillis).coerceAtLeast(0L) / 60_000L)
        return when {
            elapsedMinutes < 1L -> "たった今"
            elapsedMinutes < 60L -> "${elapsedMinutes}分前"
            elapsedMinutes < 24L * 60L -> "${elapsedMinutes / 60L}時間前"
            else -> "${elapsedMinutes / (24L * 60L)}日前"
        }
    }

    companion object {
        internal fun unavailableWorld() = WorldSnapshot(
            focusNow = 0, raidParticipants = 0, bossHp = 0, bossMaxHp = 0,
            towerFloor = 1, abyssDepth = 0, armoryReady = 0,
        )

        private const val FIREBASE_APP_NAME = "focus-raid-remote"
        private const val WORLD_COLLECTION = "world"
        private const val CURRENT_WORLD_DOCUMENT = "current"
        private const val FOOTPRINTS_COLLECTION = "footprints"
        private const val CHECKPOINTS_COLLECTION = "checkpoints"
        private const val ENTRIES_COLLECTION = "entries"
        private const val PRESET_ID_FIELD = "presetId"
        private const val CREATED_AT_FIELD = "createdAt"

        fun createOrNull(
            context: Context,
            fallback: WorldRepository = FakeWorldRepository(),
        ): FirebaseWorldRepository? {
            if (
                BuildConfig.FIREBASE_PROJECT_ID.isBlank() ||
                BuildConfig.FIREBASE_API_KEY.isBlank() ||
                BuildConfig.FIREBASE_APP_ID.isBlank()
            ) {
                return null
            }

            val app = runCatching { FirebaseApp.getInstance(FIREBASE_APP_NAME) }
                .getOrElse {
                    val options = FirebaseOptions.Builder()
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .build()
                    FirebaseApp.initializeApp(
                        context.applicationContext,
                        options,
                        FIREBASE_APP_NAME,
                    )
                }

            FirebaseAppCheckInstaller.install(app)

            return FirebaseWorldRepository(
                auth = FirebaseAuth.getInstance(app),
                firestore = FirebaseFirestore.getInstance(app),
                fallback = fallback,
            )
        }
    }
}

/** A configured but unavailable backend never falls back to simulated people. */
internal class UnavailableWorldRepository : WorldRepository {
    override val world = MutableStateFlow(FirebaseWorldRepository.unavailableWorld())
    override val syncStatus = MutableStateFlow(WorldSyncStatus.OFFLINE)
    override suspend fun refresh() = Unit
    override suspend fun footprints(expedition: Expedition, checkpoint: Int, limit: Int): List<Footprint> =
        throw java.io.IOException("Shared world unavailable")
    override suspend fun leaveFootprint(expedition: Expedition, checkpoint: Int, presetId: String): Footprint? = null
}

object WorldRepositoryFactory {
    fun create(context: Context): WorldRepository {
        val hasConfiguration = listOf(BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_APP_ID).any { it.isNotBlank() }
        if (!hasConfiguration) return FakeWorldRepository()
        return runCatching { FirebaseWorldRepository.createOrNull(context) }.getOrNull()
            ?: UnavailableWorldRepository()
    }
}
