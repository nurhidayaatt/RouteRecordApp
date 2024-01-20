package com.nurhidayaatt.routerecordapp.presentation.routes_data.statistics

import java.time.ZonedDateTime

data class StatisticState(
    val distance: Double? = null,
    val duration: Long? = null,
    val allData: List<Float> = emptyList(),
    val statisticsData: List<Float> = emptyList(),
    val type: DataType = DataType.DISTANCE,
    val label: List<ZonedDateTime> = emptyList()
)
