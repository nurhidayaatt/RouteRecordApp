package com.nurhidayaatt.routerecordapp.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.nurhidayaatt.routerecordapp.data.source.local.room.MainDao
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.domain.repository.MainRepository
import com.nurhidayaatt.routerecordapp.util.DataMapper.mapFromDataEntityToRoutes
import com.nurhidayaatt.routerecordapp.util.DataMapper.mapFromRouteToDataEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed

class MainRepositoryImpl(
    private val externalScope: CoroutineScope,
    private val client: FusedLocationProviderClient,
    private val locationRequest: LocationRequest,
    private val mainDao: MainDao,
    private val context: Context
): MainRepository {

    @SuppressLint("MissingPermission")
    override fun getLocation(): Flow<LocationResponse<Location>> = callbackFlow {
        Log.d("getLocation", "started")
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.lastOrNull()?.let {
                    trySend(LocationResponse.Success(it))
                    Log.d("getLocation1", "$it")
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Log.d("getLocation", locationAvailability.isLocationAvailable.toString())
            }
        }

        settingsClient.checkLocationSettings(builder.build()).addOnSuccessListener {
            client.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) trySend(LocationResponse.Error(exception))
        }

        awaitClose {
            Log.d("getLocation", "awaitClose")
            client.removeLocationUpdates(locationCallback)
        }
    }.shareIn(
        scope = externalScope,
        started = WhileSubscribed()
    )

    override suspend fun saveData(routes: Routes) {
        mainDao.insertRoute(data = mapFromRouteToDataEntity(routes))
    }

    override suspend fun deleteData(id: Long) {
        mainDao.deleteRoute(id = id)
    }

    override fun getRoutesData (query: SimpleSQLiteQuery): Flow<List<Routes>> = flow {
        emitAll(mainDao.getAllDataForRoutes(query = query).map { mapFromDataEntityToRoutes(it) })
    }

    override suspend fun updateImageRoute(id: Long, img: Bitmap) {
        mainDao.updateImage(id = id, img = img)
    }
}