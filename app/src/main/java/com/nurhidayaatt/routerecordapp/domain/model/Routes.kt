package com.nurhidayaatt.routerecordapp.domain.model

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

data class Routes(
    val id: Long? = null,
    val typeActivity: String? = null,
    val timestamp: LocalDateTime,
    val img: Bitmap? = null,
    val elapsedTime: Long,
    val elapsedTimePerKm: List<Long>,
    val elapsedTimePerRoute: List<Long>,
    val movingTime: Long,
    val pacePerKm: List<Long>,
    val routes: List<List<LatLng>>,
    val distance: Double,
    val distancePerRoute: List<Double>
)
