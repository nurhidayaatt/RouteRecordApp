package com.nurhidayaatt.routerecordapp.data.source.local.entity

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "data")
data class DataEntity(
    val typeActivity: String,
    val timestamp: LocalDateTime,
    val img: Bitmap,
    val elapsedTime: Long,
    val elapsedTimePerKm: DurationEntity,
    val elapsedTimePerRoute: DurationEntity,
    val movingTime: Long,
    val pacePerKm: DurationEntity,
    val routes: RoutesEntity,
    val distance: Double,
    val distancePerRoute: DistanceEntity
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
}
