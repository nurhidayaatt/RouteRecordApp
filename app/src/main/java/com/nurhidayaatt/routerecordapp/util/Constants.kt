package com.nurhidayaatt.routerecordapp.util

object Constants {
    const val NUM_TABS = 2
    const val DEFAULT_DELAY = 500L
    const val CANCEL_SERVICE_DELAY = 1000L
    const val SNAPSHOT_DELAY = 2000L
    const val DEFAULT_INTERVAL = 1000L

    const val ACTION_SERVICE_START = "ACTION_SERVICE_START"
    const val ACTION_SERVICE_PAUSE = "ACTION_SERVICE_PAUSE"
    const val ACTION_SERVICE_FINISH = "ACTION_SERVICE_FINISH"
    const val ACTION_SERVICE_CANCEL = "ACTION_SERVICE_CANCEL"

    const val LOCATION_TRACKING_STATE = "LOCATION_TRACKING_STATE"

    const val NOTIFICATION_CHANNEL_ID = "LOCATION_TRACKING_NOTIFICATION_ID"
    const val NOTIFICATION_CHANNEL_NAME = "Tracking Route Notification"
    const val NOTIFICATION_ID = 1

    const val CLICK_REQUEST_CODE = 100
    const val CANCEL_REQUEST_CODE = 101
    const val PAUSE_REQUEST_CODE = 102
    const val FINISH_REQUEST_CODE = 103
    const val RESUME_REQUEST_CODE = 104

    const val ROUTE_DATABASE_NAME = "routes_db"
    const val USER_PREFERENCES = "user_preference"

    val BOTTOM_SHEET_STATE = listOf(
        "STATE_DRAGGING",
        "STATE_SETTLING",
        "STATE_EXPANDED",
        "STATE_COLLAPSED",
        "STATE_HIDDEN",
        "STATE_HALF_EXPANDED"
    )
}