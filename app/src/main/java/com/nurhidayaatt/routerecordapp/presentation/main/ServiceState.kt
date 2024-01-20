package com.nurhidayaatt.routerecordapp.presentation.main

import com.nurhidayaatt.routerecordapp.services.LocationTrackingState

data class ServiceState(
    val currentState: LocationTrackingState = LocationTrackingState.Idle,
    val lastState: LocationTrackingState = LocationTrackingState.Started
)
