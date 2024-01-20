package com.nurhidayaatt.routerecordapp.domain.repository

import android.graphics.Bitmap
import android.location.Location
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.gms.common.api.ResolvableApiException
import com.nurhidayaatt.routerecordapp.data.LocationResponse
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    fun getLocation(): Flow<LocationResponse<Location>>
    suspend fun saveData(routes: Routes)
    suspend fun deleteData(id: Long)
    fun getRoutesData (query: SimpleSQLiteQuery): Flow<List<Routes>>
    suspend fun updateImageRoute(id: Long, img: Bitmap)
}