package com.nurhidayaatt.routerecordapp.services

import android.location.Location
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.model.LatLng
import kotlin.time.Duration

data class ServiceData(
    val state: LocationTrackingState = LocationTrackingState.Idle,
    val duration: Duration = Duration.ZERO,
    val elapsedTimePerKm: List<Duration> = listOf(Duration.ZERO),
    val elapsedTimePerRoute: List<Duration> = listOf(Duration.ZERO),
    val movingTime: Duration = Duration.ZERO,
    val pacePerKm: List<Duration> = listOf(Duration.ZERO),
    val routes: List<List<LatLng>> = listOf(),
    val distancePerRoute: List<Double> = listOf(),
    val lastLocation: Location? = null,
    val speeds:List<Float> = listOf(),
    val exception: ResolvableApiException? = null
)

