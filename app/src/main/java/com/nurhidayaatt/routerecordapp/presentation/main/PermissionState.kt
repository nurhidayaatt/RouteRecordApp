package com.nurhidayaatt.routerecordapp.presentation.main

data class PermissionState(
    val locationPermissionGranted: Boolean = false,
    val myLocationEnabled: Boolean = false,
    val postNotificationPermissionGranted: Boolean = true,
    val backgroundLocationPermissionGranted: Boolean = true,
    val showAlertDialog: Boolean = false,
)
