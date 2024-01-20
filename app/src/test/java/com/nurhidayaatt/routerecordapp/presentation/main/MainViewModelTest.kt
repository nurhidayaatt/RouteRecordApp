package com.nurhidayaatt.routerecordapp.presentation.main

import android.location.Location
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.common.ConnectionResult
import com.nurhidayaatt.routerecordapp.data.LocationResponse
import com.nurhidayaatt.routerecordapp.domain.repository.MainRepository
import com.nurhidayaatt.routerecordapp.services.LocationTrackingState
import com.nurhidayaatt.routerecordapp.util.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val googleServicesAvailability: Int = ConnectionResult.SUCCESS

    @Mock
    private lateinit var mainRepository: MainRepository

    @Mock
    private lateinit var userPreferences: DataStore<Preferences>

    @Mock
    private lateinit var location: Location
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setUp() {
        mainViewModel = MainViewModel(googleServicesAvailability, mainRepository, userPreferences)
    }

    @Test
    fun `permission location not granted then change permission state`() = runTest {
        val expectedPermissionState = PermissionState(
            locationPermissionGranted = false,
            showAlertDialog = true,
            myLocationEnabled = false
        )
        mainViewModel.updateLocationPermission(isGranted = false).invokeOnCompletion {
            val actualPermissionState = mainViewModel.permissionState.value
            assertEquals(expectedPermissionState, actualPermissionState)
        }
    }

    @Test
    fun `permission location granted then change permission state`() = runTest {
        val expectedPermissionState = PermissionState(
            locationPermissionGranted = true,
            showAlertDialog = false,
            myLocationEnabled = true
        )
        mainViewModel.updateLocationPermission(isGranted = true).invokeOnCompletion {
            val actualPermissionState = mainViewModel.permissionState.value
            assertEquals(expectedPermissionState, actualPermissionState)
        }
    }

    @Test
    fun `permission notification not granted then change permission state`() = runTest {
        val expectedPermissionState =
            PermissionState(postNotificationPermissionGranted = false, showAlertDialog = true)
        try {
            mainViewModel.updateNotificationPermission(isGranted = false)
        } finally {
            val actualPermissionState = mainViewModel.permissionState.value
            assertEquals(expectedPermissionState, actualPermissionState)
        }
    }

    @Test
    fun `permission notification granted then change permission state`() = runTest {
        val expectedPermissionState =
            PermissionState(postNotificationPermissionGranted = true, showAlertDialog = false)
        try {
            mainViewModel.updateNotificationPermission(isGranted = true)
        } finally {
            val actualPermissionState = mainViewModel.permissionState.value
            assertEquals(expectedPermissionState, actualPermissionState)
        }
    }

    @Test
    fun `permission background location not granted then change permission state`() = runTest {
        val expectedPermissionState =
            PermissionState(backgroundLocationPermissionGranted = false, showAlertDialog = true)
        try {
            mainViewModel.updateBackgroundLocationPermission(isGranted = false)
        } finally {
            val actualPermissionState = mainViewModel.permissionState.value
            assertEquals(expectedPermissionState, actualPermissionState)
        }
    }

    @Test
    fun `permission background location granted then change permission state and service state`() =
        runTest {
            val expectedPermissionState =
                PermissionState(backgroundLocationPermissionGranted = true, showAlertDialog = false)
            val expectedServiceState = ServiceState(
                currentState = LocationTrackingState.Started,
                lastState = LocationTrackingState.Started
            )
            try {
                mainViewModel.updateBackgroundLocationPermission(isGranted = true)
            } finally {
                val actualPermissionState = mainViewModel.permissionState.value
                val actualServiceState = mainViewModel.serviceState.value
                assertEquals(expectedPermissionState, actualPermissionState)
                assertEquals(expectedServiceState, actualServiceState)
            }
        }

    @Test
    fun `hide alert dialog`() = runTest {
        val expectedShowAlertDialog = false
        try {
            mainViewModel.showAlertDialog(show = false)
        } finally {
            val actualShowAlertDialog = mainViewModel.permissionState.value.showAlertDialog
            assertEquals(expectedShowAlertDialog, actualShowAlertDialog)
        }
    }

    @Test
    fun `show alert dialog`() = runTest {
        try {
            mainViewModel.showAlertDialog(show = true)
        } finally {
            val actualShowAlertDialog = mainViewModel.permissionState.value.showAlertDialog
            assertEquals(true, actualShowAlertDialog)
        }
    }

    @Test
    fun `show my location without location permission`() = runTest {
        val expectedShowAlertDialog = true
        try {
            mainViewModel.showMyLocation()
        } finally {
            val actualShowAlertDialog = mainViewModel.permissionState.value.showAlertDialog
            assertEquals(expectedShowAlertDialog, actualShowAlertDialog)
        }
    }

    @Test
    fun `show my location with location permission`() = runTest {
        val locationFlow = flow { emit(LocationResponse.Success(location)) }
        `when`(mainRepository.getLocation()).thenReturn(locationFlow)
        val expectedShowAlertDialog = false
        val expectedCurrentLocationState = locationFlow.first()
        try {
            mainViewModel.updateLocationPermission(isGranted = true).invokeOnCompletion {
                mainViewModel.showMyLocation()
            }
        } finally {
            val actualCurrentLocationState = mainViewModel.currentLocationState.take(1).first()
            val actualShowAlertDialog = mainViewModel.permissionState.value.showAlertDialog
            assertEquals(expectedShowAlertDialog, actualShowAlertDialog)
            assertEquals(
                expectedCurrentLocationState.data?.latitude,
                actualCurrentLocationState.data?.latitude
            )
        }
    }

    @Test
    fun `datastore preference update and return ActivityType cycling`() = runTest {
        val expectedUserPreferences = ActivityType.CYCLING
        mainViewModel.updateUserPref(activityType = ActivityType.CYCLING)
        mainViewModel.userPref.distinctUntilChangedBy { actualUserPreferences ->
            assertEquals(expectedUserPreferences.name, actualUserPreferences.name)
        }
    }

    @Test
    fun `datastore preference update and return ActivityType running`() = runTest {
        val expectedUserPreferences = ActivityType.RUNNING
        mainViewModel.updateUserPref(activityType = ActivityType.RUNNING)
        mainViewModel.userPref.distinctUntilChangedBy { actualUserPreferences ->
            assertEquals(expectedUserPreferences.name, actualUserPreferences.name)
        }
    }


}