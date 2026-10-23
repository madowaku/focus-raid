package com.madowaku.focusraid.walking

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HealthConnectStepsRepository(private val context: Context) : StepsRepository {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }
    override fun availability() = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> StepsAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> StepsAvailability.UPDATE_REQUIRED
        else -> StepsAvailability.UNAVAILABLE
    }
    override suspend fun hasPermission() = client.permissionController.getGrantedPermissions().containsAll(permissions)
    override suspend fun read(start: Instant, end: Instant): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(start, end),
        ),
    )[StepsRecord.COUNT_TOTAL]

    companion object {
        val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    }
}
