package com.nurhidayaatt.routerecordapp.presentation.routes_data.statistics

import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterActivity
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterTime

data class StatisticPreferences(
    val dataset: Dataset = Dataset.AVERAGE,
    val dataType: DataType = DataType.DISTANCE,
    val filterTime: FilterTime = FilterTime.ALL_TIME,
    val filterActivity: FilterActivity = FilterActivity.ALL_ACTIVITY
)

enum class Dataset { AVERAGE, TOTAL }
enum class DataType { DISTANCE, DURATION }
