package com.nurhidayaatt.routerecordapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nurhidayaatt.routerecordapp.data.MainRepositoryImpl
import com.nurhidayaatt.routerecordapp.data.source.local.room.MainDao
import com.nurhidayaatt.routerecordapp.data.source.local.room.MainDatabase
import com.nurhidayaatt.routerecordapp.domain.repository.MainRepository
import com.nurhidayaatt.routerecordapp.presentation.RouteRecordApp
import com.nurhidayaatt.routerecordapp.util.Constants.DEFAULT_INTERVAL
import com.nurhidayaatt.routerecordapp.util.Constants.ROUTE_DATABASE_NAME
import com.nurhidayaatt.routerecordapp.util.Constants.USER_PREFERENCES
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Singleton
    @GoogleServicesAvailability
    @Provides
    fun provideGoogleApiAvailability(@ApplicationContext context: Context): Int {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    }

    @Singleton
    @Provides
    fun provideFusedLocationClientProvider(
        @ApplicationContext context: Context,
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Singleton
    @Provides
    fun provideLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, DEFAULT_INTERVAL).build()
    }

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): MainDatabase {
        return Room.databaseBuilder(
            context,
            MainDatabase::class.java,
            ROUTE_DATABASE_NAME
        ).build()
    }

    @Singleton
    @Provides
    fun provideRunDao(db: MainDatabase): MainDao {
        return db.mainDao()
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            scope = (context.applicationContext as RouteRecordApp).applicationScope,
            produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES) }
        )
    }

    @Singleton
    @Provides
    fun provideLocationTracker(
        @ApplicationContext context: Context,
        client: FusedLocationProviderClient,
        locationRequest: LocationRequest,
        mainDao: MainDao,
    ): MainRepository {
        return MainRepositoryImpl(
            externalScope = (context.applicationContext as RouteRecordApp).applicationScope,
            client = client,
            locationRequest = locationRequest,
            mainDao = mainDao,
            context = context
        )
    }
}