package com.nurhidayaatt.routerecordapp.presentation.main

import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.ConnectionResult
import com.nurhidayaatt.routerecordapp.data.LocationResponse
import com.nurhidayaatt.routerecordapp.di.GoogleServicesAvailability
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.domain.repository.MainRepository
import com.nurhidayaatt.routerecordapp.services.LocationTrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @GoogleServicesAvailability
    private val googleServicesAvailability: Int,
    private val mainRepository: MainRepository,
    private val userPreferences: DataStore<Preferences>
) : ViewModel() {

    private val _isServicesAvailable = MutableStateFlow(value = false)
    val isServicesAvailable: StateFlow<Boolean> = _isServicesAvailable

    private val _permissionState = MutableStateFlow(value = PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState

    private val currentLocationChannel = Channel<LocationResponse<Location>>()
    val currentLocationState = currentLocationChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var locationJob: Job? = null

    private val _boundServiceConnected = MutableStateFlow(value = false)
    val boundServiceConnected: StateFlow<Boolean> = _boundServiceConnected

    private val _serviceState = MutableStateFlow(value = ServiceState())
    val serviceState: StateFlow<ServiceState> = _serviceState

    init {
        _isServicesAvailable.update { googleServicesAvailability == ConnectionResult.SUCCESS }
    }

    // Perms State
    //----------------------------------------------------------------------------------------------
    fun updateLocationPermission(isGranted: Boolean) = viewModelScope.launch {
        if (!permissionState.value.locationPermissionGranted) {
            _permissionState.update {
                it.copy(
                    locationPermissionGranted = isGranted,
                    showAlertDialog = !isGranted,
                    myLocationEnabled = isGranted
                )
            }
            /*if (!isGranted) return
            getCurrentLocation()*/
        }
    }

    fun updateNotificationPermission(isGranted: Boolean) {
        _permissionState.update {
            it.copy(
                postNotificationPermissionGranted = isGranted,
                showAlertDialog = !isGranted
            )
        }
    }

    fun updateBackgroundLocationPermission(isGranted: Boolean) {
        _permissionState.update {
            it.copy(
                backgroundLocationPermissionGranted = isGranted,
                showAlertDialog = !isGranted
            )
        }
        if (!isGranted) return
        updateLocationServiceState(LocationTrackingState.Started)
    }

    fun showAlertDialog(show: Boolean) {
        _permissionState.update { it.copy(showAlertDialog = show) }
    }

    // UI State
    //----------------------------------------------------------------------------------------------
    fun showMyLocation(/*show: Boolean*/) {
        //if (show) {
            if (!permissionState.value.locationPermissionGranted) {
                showAlertDialog(show = true)
                return
            }
            getCurrentLocation()
        /*} else {
            _uiState.update { state ->
                state.copy(currentLocation = null)
            }
        }*/
    }

    private fun getCurrentLocation() {
        if (locationJob != null) return
        locationJob = mainRepository.getLocation().take(1).onEach {
            /*_uiState.update { state ->
                state.copy(currentLocation = it)
            }*/
            currentLocationChannel.send(element = it)
        }.onCompletion {
            if (it != null) {
                Log.e("getLocation: throwable", it.toString())
            }
            locationJob?.cancel()
            locationJob = null
        }.launchIn(viewModelScope)
    }

    val userPref = userPreferences.data.map { pref: Preferences ->
        ActivityType.valueOf(value = pref[ACTIVITY_TYPE] ?: ActivityType.RUNNING.name)
    }

    fun updateUserPref(activityType: ActivityType) = viewModelScope.launch {
        userPreferences.edit { preferences ->
            preferences[ACTIVITY_TYPE] = activityType.name
        }
    }

    val myLocationEnabled = permissionState.combine(uiState) { permissionState: PermissionState, uiState: UiState ->
        permissionState.myLocationEnabled && !uiState.showRoutePreview
    }

    val showMainBottomSheet = serviceState.combine(uiState) { serviceState: ServiceState, uiState: UiState ->
        serviceState.currentState == LocationTrackingState.Idle &&
                !uiState.showDataRoutes &&
                !uiState.showRoutePreview
    }

    val hideFab = serviceState.combine(uiState) { serviceState: ServiceState, uiState: UiState ->
        serviceState.currentState != LocationTrackingState.Idle ||
                uiState.showDataRoutes ||
                uiState.showRoutePreview
    }

    fun showDataRoutes(show: Boolean) {
        _uiState.update { it.copy(showDataRoutes = show) }
    }

    fun showRoutePreview(show: Boolean) {
        _uiState.update { it.copy(showRoutePreview = show) }
    }

    fun updateImage(id: Long, img: Bitmap) {
        viewModelScope.launch {
            mainRepository.updateImageRoute(id = id, img = img)
        }
    }

    // Service State
    //----------------------------------------------------------------------------------------------
    fun updateBoundServices(isConnected: Boolean) {
        _boundServiceConnected.update { isConnected }
    }

    fun updateLastStateActiveService(state: LocationTrackingState) {
        _serviceState.update { it.copy(lastState = state) }
    }

    fun updateLocationServiceState(state: LocationTrackingState) {
        if (state == LocationTrackingState.Started || state == LocationTrackingState.Paused) {
            _serviceState.update { it.copy(currentState = state, lastState = state) }
        } else {
            _serviceState.update { it.copy(currentState = state) }
        }
    }

    fun saveData(routes: Routes) = viewModelScope.launch {
        mainRepository.saveData(routes = routes)
    }

    override fun onCleared() {
        if (locationJob != null) {
            locationJob?.cancel()
            locationJob = null
        }
        super.onCleared()
    }
}