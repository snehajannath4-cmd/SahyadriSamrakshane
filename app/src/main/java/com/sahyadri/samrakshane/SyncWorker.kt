package com.sahyadri.samrakshane

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val db = AlertDatabase.getInstance(applicationContext)
            val unsyncedAlerts = db.alertDao().getUnsyncedAlerts()
            val database = FirebaseDatabase.getInstance().reference

            for (alert in unsyncedAlerts) {
                val alertId = database.child("alerts").push().key ?: continue

                val alertData = hashMapOf(
                    "type" to alert.type,
                    "description" to alert.description,
                    "latitude" to alert.latitude,
                    "longitude" to alert.longitude,
                    "timestamp" to alert.timestamp,
                    "status" to "Reported"
                )

                database.child("alerts").child(alertId).setValue(alertData)
                db.alertDao().markAsSynced(alert.id)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}