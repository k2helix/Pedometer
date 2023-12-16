package de.j4velin.pedometer

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnect {
    fun saveStepsToHealthConnect(steps: Int, startTime: Long, endTime: Long, context: Context) {
        val availabilityStatus = HealthConnectClient.getSdkStatus(context)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE || availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            return // early return as there is no viable integration
        }

        val healthConnectClient = HealthConnectClient.getOrCreate(context)
        val PERMISSIONS =
                setOf(
                        HealthPermission.getReadPermission(StepsRecord::class),
                        HealthPermission.getWritePermission(StepsRecord::class)
                )

        runBlocking {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) {
                try {
                    val userTimeZone: ZoneId = ZoneId.systemDefault()
                    val userZoneOffset: ZoneOffset = userTimeZone.rules.getOffset(Instant.ofEpochMilli(startTime))
                    val stepsRecord = StepsRecord(
                            count = steps.toLong(),
                            startTime = Instant.ofEpochMilli(startTime),
                            endTime = Instant.ofEpochMilli(endTime),
                            startZoneOffset = userZoneOffset,
                            endZoneOffset = userZoneOffset

                    )
                    healthConnectClient.insertRecords(listOf(stepsRecord))
                } catch (e: Exception) {
                    e.message?.let { Log.e("HealthConnect", it) }
                }
            }
        }

    }

}