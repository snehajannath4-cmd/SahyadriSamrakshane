package com.sahyadri.samrakshane

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    val type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isSynced: Boolean = false,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)