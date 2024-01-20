package com.nurhidayaatt.routerecordapp.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.domain.repository.MainRepository
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterActivity
import com.nurhidayaatt.routerecordapp.presentation.routes_data.FilterTime
import com.nurhidayaatt.routerecordapp.presentation.routes_data.routes.RoutesPreferences
import com.nurhidayaatt.routerecordapp.presentation.routes_data.routes.SortType
import com.nurhidayaatt.routerecordapp.presentation.routes_data.statistics.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*
import javax.inject.Inject
import kotlin.collections.List

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val mainRepository: MainRepository,
) : ViewModel() {

    private val _route = MutableStateFlow(SharedState())
    val route: StateFlow<SharedState> = _route

    fun setRoute(routes: List<Routes>? = emptyList(), position: Int? = null) {
        _route.update { it.copy(routes = routes, position = position, needDelay = true) }
    }

    fun updatePosition(position: Int) {
        _route.update { it.copy(position = position, needDelay = false) }
    }

    private val firstDayOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear())
    private val firstDayOfNextYear = LocalDate.now().with(TemporalAdjusters.firstDayOfNextYear())
    private val firstDayOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
    private val firstDayOfNextMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfNextMonth())
    private val firstDayOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val firstDayOfNextWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

    // Routes View
    private val _routePref = MutableStateFlow(RoutesPreferences())
    val routePref: StateFlow<RoutesPreferences> = _routePref

    fun updateSortTypeRoute(sortType: SortType) {
        _routePref.update { it.copy(sortType = sortType) }
    }

    fun updateFilterTimeRoute(filterTime: FilterTime) {
        _routePref.update { it.copy(filterTime = filterTime) }
    }

    fun updateFilterActivity(filterActivity: FilterActivity) {
        _routePref.update { it.copy(filterActivity = filterActivity) }
    }

    fun deleteData(routesId: Long) = viewModelScope.launch {
        mainRepository.deleteData(id = routesId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val routesData = routePref.flatMapLatest { pref ->
        val query = StringBuilder().append("SELECT * FROM ")
        when (pref.sortType) {
            SortType.TIMESTAMP_DESC -> query.append("(SELECT * FROM data ORDER BY timestamp DESC) ")
            SortType.TIMESTAMP_ASC -> query.append("(SELECT * FROM data ORDER BY timestamp ASC) ")
            SortType.DURATION_DESC -> query.append("(SELECT * FROM data ORDER BY elapsedTime DESC) ")
            SortType.DURATION_ASC -> query.append("(SELECT * FROM data ORDER BY elapsedTime ASC) ")
            SortType.DISTANCE_DESC -> query.append("(SELECT * FROM data ORDER BY distance DESC) ")
            SortType.DISTANCE_ASC -> query.append("(SELECT * FROM data ORDER BY distance ASC) ")
        }
        when (pref.filterActivity) {
            FilterActivity.ALL_ACTIVITY -> query.append("WHERE")
            FilterActivity.RUNNING -> query.append("WHERE typeActivity = '${FilterActivity.RUNNING.name}' AND")
            FilterActivity.CYCLING -> query.append("WHERE typeActivity = '${FilterActivity.CYCLING.name}' AND")
        }
        val simpleSQLiteQuery = when (pref.filterTime) {
            FilterTime.ALL_TIME -> {
                SimpleSQLiteQuery(query.substring(0, query.lastIndexOf(" ")).toString())
            }
            FilterTime.LAST_WEEK -> {
                query.append(" (timestamp BETWEEN ? AND ?)")
                SimpleSQLiteQuery(
                    query.toString(),
                    arrayOf(firstDayOfWeek.toString(), firstDayOfNextWeek.toString())
                )
            }
            FilterTime.LAST_MONTH -> {
                query.append(" (timestamp BETWEEN ? AND ?)")
                SimpleSQLiteQuery(
                    query.toString(),
                    arrayOf(firstDayOfMonth.toString(), firstDayOfNextMonth.toString())
                )
            }
            FilterTime.LAST_YEAR -> {
                query.append(" (timestamp BETWEEN ? AND ?)")
                SimpleSQLiteQuery(
                    query.toString(),
                    arrayOf(firstDayOfYear.toString(), firstDayOfNextYear.toString())
                )
            }
        }
        Log.d("Routes Query", simpleSQLiteQuery.sql)
        mainRepository.getRoutesData(simpleSQLiteQuery)
    }

    // Statistics View
    private val _statisticPrefs = MutableStateFlow(StatisticPreferences())
    val statisticPrefs: StateFlow<StatisticPreferences> = _statisticPrefs

    fun updateFilterTimeStatistic(filterTime: FilterTime) {
        _statisticPrefs.update { it.copy(filterTime = filterTime) }
    }

    fun updateFilterActivityStatistic(filterActivity: FilterActivity) {
        _statisticPrefs.update { it.copy(filterActivity = filterActivity) }
    }

    fun updateDatasetStatistic(dataset: Dataset) {
        _statisticPrefs.update { it.copy(dataset = dataset) }
    }

    fun updateDataTypeStatistic(dataType: DataType) {
        _statisticPrefs.update { it.copy(dataType = dataType) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val statisticData = statisticPrefs.flatMapLatest { pref ->
        val query = StringBuilder().append("SELECT * FROM data ")
        when (pref.filterActivity) {
            FilterActivity.ALL_ACTIVITY -> query.append("WHERE")
            FilterActivity.RUNNING -> query.append("WHERE typeActivity = '${FilterActivity.RUNNING.name}' AND")
            FilterActivity.CYCLING -> query.append("WHERE typeActivity = '${FilterActivity.CYCLING.name}' AND")
        }
        val simpleSQLiteQuery = when (pref.filterTime) {
            FilterTime.ALL_TIME -> {
                val newQuery = StringBuilder().append(query.substring(0, query.lastIndexOf(" ")))
                newQuery.append(" ORDER BY timestamp DESC")
                SimpleSQLiteQuery(newQuery.toString())
            }
            FilterTime.LAST_WEEK -> {
                query.append(" (timestamp BETWEEN ? AND ?) ORDER BY timestamp DESC")
                SimpleSQLiteQuery(
                    query.toString(),
                    arrayOf(firstDayOfWeek.toString(), firstDayOfNextWeek.toString())
                )
            }
            FilterTime.LAST_MONTH -> {
                query.append(" (timestamp BETWEEN ? AND ?) ORDER BY timestamp DESC")
                SimpleSQLiteQuery(
                    query.toString(),
                    arrayOf(firstDayOfMonth.toString(), firstDayOfNextMonth.toString())
                )
            }
            FilterTime.LAST_YEAR -> {
                query.append(" (timestamp BETWEEN ? AND ?) ORDER BY timestamp DESC")
                SimpleSQLiteQuery(
                    query.toString(),
                    arrayOf(firstDayOfYear.toString(), firstDayOfNextYear.toString())
                )
            }
        }
        Log.d("Statistic Query", simpleSQLiteQuery.sql)
        mainRepository.getRoutesData(simpleSQLiteQuery).map { mapRoutesToStatisticsData(pref, it) }
    }

    private fun mapRoutesToStatisticsData(pref: StatisticPreferences, data: List<Routes>): StatisticState {
        return when (pref.dataset) {
            Dataset.AVERAGE -> {
                StatisticState(
                    distance = if (data.isNotEmpty()) {
                        data.sumOf { it.distance } / data.size
                    } else null,
                    duration = if (data.isNotEmpty()) {
                        data.sumOf { it.elapsedTime } / data.size
                    } else null,
                    statisticsData = if (pref.dataType == DataType.DURATION) {
                        data.map { it.elapsedTime.toFloat() }
                    } else {
                        data.map { it.distance.toFloat() }
                    },
                    type = if (pref.dataType == DataType.DURATION) DataType.DURATION else DataType.DISTANCE,
                    label = data.map {
                        it.timestamp.atZone(ZoneId.systemDefault())
                    }
                )
            }

            Dataset.TOTAL -> {
                StatisticState(
                    distance = if (data.isNotEmpty()) {
                        data.sumOf { it.distance }
                    } else null,
                    duration = if (data.isNotEmpty()) {
                        data.sumOf { it.elapsedTime }
                    } else null,
                    statisticsData = if (pref.dataType == DataType.DURATION) {
                        data.map { it.elapsedTime.toFloat() }
                    } else {
                        data.map { it.distance.toFloat() }
                    },
                    type = if (pref.dataType == DataType.DURATION) DataType.DURATION else DataType.DISTANCE,
                    label = data.map {
                        it.timestamp.atZone(ZoneId.systemDefault())
                    }
                )
            }
        }
    }
}
