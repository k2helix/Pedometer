package de.j4velin.pedometer

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.units.Length
import de.j4velin.pedometer.ui.Fragment_Settings
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnect {
    fun saveToHealthConnect(steps: Int, startTime: Long, endTime: Long, context: Context) {
        val availabilityStatus = HealthConnectClient.getSdkStatus(context)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE || availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            return // early return as there is no viable integration
        }

        val healthConnectClient = HealthConnectClient.getOrCreate(context)
        val permissions =
                setOf(
                        HealthPermission.getReadPermission(StepsRecord::class),
                        HealthPermission.getWritePermission(StepsRecord::class),
                        HealthPermission.getReadPermission(DistanceRecord::class),
                        HealthPermission.getWritePermission(DistanceRecord::class)
                )

        runBlocking {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(permissions)) {
                try {
                    val userTimeZone: ZoneId = ZoneId.systemDefault()
                    val userZoneOffset: ZoneOffset = userTimeZone.rules.getOffset(Instant.ofEpochMilli(startTime))
                    val prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    val stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE)
                    var distance = steps * stepsize
                    val distanceLength: Length

                    if (prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT) == "cm") {
                        distance /= 100000f
                        distanceLength = Length.kilometers(distance.toDouble())
                    } else {
                        distance /= 5280f
                        distanceLength = Length.miles(distance.toDouble())
                    }

                    val stepsRecord = StepsRecord(
                            count = steps.toLong(),
                            startTime = Instant.ofEpochMilli(startTime),
                            endTime = Instant.ofEpochMilli(endTime),
                            startZoneOffset = userZoneOffset,
                            endZoneOffset = userZoneOffset
                    )
                    val distanceRecord = DistanceRecord(
                            startTime = Instant.ofEpochMilli(startTime),
                            endTime = Instant.ofEpochMilli(endTime),
                            startZoneOffset = userZoneOffset,
                            endZoneOffset = userZoneOffset,
                            distance = distanceLength
                    )

                    healthConnectClient.insertRecords(listOf(stepsRecord, distanceRecord))
                } catch (e: Exception) {
                    e.message?.let { Log.e("HealthConnect", it) }
                }
            }
        }

    }

}