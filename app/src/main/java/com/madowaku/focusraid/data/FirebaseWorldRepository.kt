package com.madowaku.focusraid.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.madowaku.focusraid.BuildConfig
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Footprint
import com.madowaku.focusraid.core.model.WorldSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseWorldRepository private constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val fallback: WorldRepository,
) : WorldRepository {
    private val _world = MutableStateFlow(fallback.snapshot())
    override val world: StateFlow<WorldSnapshot> = _world.asStateFlow()

    private val _syncStatus = MutableStateFlow(WorldSyncStatus.CONNECTING)
    override val syncStatus: StateFlow<WorldSyncStatus> = _syncStatus.asStateFlow()

    override suspend fun refresh() {
        _syncStatus.value = WorldSyncStatus.CONNECTING
        runCatching {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            val document = firestore
                .collection(WORLD_COLLECTION)
                .document(CURRENT_WORLD_DOCUMENT)
                .get(Source.SERVER)
                .await()
            check(document.exists()) { "world/current does not exist" }
            _world.value = RemoteWorldMapping.fromMap(
                values = document.data.orEmpty(),
                fallback = fallback.snapshot(),
            )
            _syncStatus.value = WorldSyncStatus.LIVE
        }.onFailure {
            _world.value = fallback.snapshot()
            _syncStatus.value = WorldSyncStatus.OFFLINE
        }
    }

    override fun footprints(
        expedition: Expedition,
        checkpoint: Int,
        limit: Int,
    ): List<Footprint> = fallback.footprints(expedition, checkpoint, limit)

    override fun leaveFootprint(
        expedition: Expedition,
        checkpoint: Int,
        presetId: String,
    ): Footprint? = fallback.leaveFootprint(expedition, checkpoint, presetId)

    companion object {
        private const val FIREBASE_APP_NAME = "focus-raid-remote"
        private const val WORLD_COLLECTION = "world"
        private const val CURRENT_WORLD_DOCUMENT = "current"

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
                ?: return null

            return FirebaseWorldRepository(
                auth = FirebaseAuth.getInstance(app),
                firestore = FirebaseFirestore.getInstance(app),
                fallback = fallback,
            )
        }
    }
}

object WorldRepositoryFactory {
    fun create(context: Context): WorldRepository {
        val fallback = FakeWorldRepository()
        return FirebaseWorldRepository.createOrNull(context, fallback) ?: fallback
    }
}
