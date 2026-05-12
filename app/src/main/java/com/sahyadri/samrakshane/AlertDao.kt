package com.sahyadri.samrakshane

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AlertDao {

    @Insert
    fun insertAlert(alert: AlertEntity)

    @Query("SELECT * FROM alerts WHERE isSynced = 0")
    fun getUnsyncedAlerts(): List<AlertEntity>

    @Query("UPDATE alerts SET isSynced = 1 WHERE id = :id")
    fun markAsSynced(id: Int)
}