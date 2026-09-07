package com.madowaku.focusraid.billing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ProAccessCache {
    fun read(): BillingSnapshot?
    suspend fun write(snapshot: BillingSnapshot)
}

class LocalProAccessCache(context: Context) : ProAccessCache {
    private val prefs = context.applicationContext.getSharedPreferences("verified_pro", Context.MODE_PRIVATE)
    override fun read(): BillingSnapshot? = if (!prefs.contains("is_pro")) null else BillingSnapshot(
        isPro = prefs.getBoolean("is_pro", false), verifiedAtMillis = prefs.getLong("verified_at", 0),
    )
    override suspend fun write(snapshot: BillingSnapshot) = withContext(Dispatchers.IO) {
        check(prefs.edit().putBoolean("is_pro", snapshot.isPro)
            .putLong("verified_at", snapshot.verifiedAtMillis).commit())
    }
}
