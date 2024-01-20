package com.nurhidayaatt.routerecordapp.presentation.routes_data.routes

import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterActivity
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterTime

data class RoutesPreferences(
    val sortType: SortType = SortType.TIMESTAMP_DESC,
    val filterTime: FilterTime = FilterTime.ALL_TIME,
    val filterActivity: FilterActivity = FilterActivity.ALL_ACTIVITY
)

enum class SortType {
    TIMESTAMP_DESC, TIMESTAMP_ASC, DURATION_DESC, DURATION_ASC, DISTANCE_DESC, DISTANCE_ASC
}
