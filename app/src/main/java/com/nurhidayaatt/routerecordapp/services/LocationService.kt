package com.nurhidayaatt.routerecordapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.nurhidayaatt.routerecordapp.R
import com.nurhidayaatt.routerecordapp.data.LocationResponse
import com.nurhidayaatt.routerecordapp.domain.repository.MainRepository
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_CANCEL
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_FINISH
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_PAUSE
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_START
import com.nurhidayaatt.routerecordapp.util.Constants.LOCATION_TRACKING_STATE
import com.nurhidayaatt.routerecordapp.util.Constants.NOTIFICATION_CHANNEL_ID
import com.nurhidayaatt.routerecordapp.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.nurhidayaatt.routerecordapp.util.Constants.NOTIFICATION_ID
import com.nurhidayaatt.routerecordapp.util.formatDistance
import com.nurhidayaatt.routerecordapp.util.durationToTimerFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var mainRepository: MainRepository
    @Inject
    lateinit var notificationManager: NotificationManager
    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    private val serviceScope = CoroutineScope(context = SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private lateinit var timer: Timer
    private var duration: Duration = ZERO
    private var durationTemp: Duration = ZERO
    private var movingTime: Duration = ZERO
    private var durationPerKm: MutableList<Duration> = mutableListOf(ZERO)
    private var pacePerKm: MutableList<Duration> = mutableListOf(ZERO)
    private var lastLocation: Location? = null
    private var speeds: MutableList<Float> = mutableListOf()

    // Per Route
    private var durationPerRoute: MutableList<Duration> = mutableListOf()
    private var polyLines: MutableList<MutableList<LatLng>> = mutableListOf()
    private var distances: MutableList<Double> = mutableListOf()

    private var exception: ResolvableApiException? = null

    private val _serviceData = MutableStateFlow(ServiceData())
    val serviceData: StateFlow<ServiceData> = _serviceData

    private val _lastState = MutableStateFlow(LocationTrackingState.Started)
    val lastState: StateFlow<LocationTrackingState> = _lastState

    private val binder = LocationBinder()

    inner class LocationBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notification Action
        when (intent?.getStringExtra(LOCATION_TRACKING_STATE)) {
            LocationTrackingState.Started.name -> {
                _lastState.update { LocationTrackingState.Started }
                start()
            }
            LocationTrackingState.Paused.name -> {
                _lastState.update { LocationTrackingState.Paused }
                pause()
            }
        }
        when (intent?.action) {
            ACTION_SERVICE_START -> start()
            ACTION_SERVICE_PAUSE -> pause()
            ACTION_SERVICE_FINISH -> finish()
            ACTION_SERVICE_CANCEL -> cancel()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        if (serviceData.value.state == LocationTrackingState.Started) return
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        _serviceData.update { it.copy(state = LocationTrackingState.Started) }
        setNotificationButton()
        startTracking { duration, distances ->
            updateNotification(duration = duration, distances = distances)
        }
    }

    private fun pause() {
        _serviceData.update { it.copy(state = LocationTrackingState.Paused) }
        stopTracking()
        setNotificationButton()
    }

    private fun finish() {
        _serviceData.update { it.copy(state = LocationTrackingState.Finished) }
        stopTracking()
    }

    private fun cancel() {
        stopTracking()
        resetAllData()
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTracking(onChange: (duration: Duration, distances: MutableList<Double>) -> Unit) {
        if (job != null) return
        _serviceData.update { it.copy(exception = null) }
        val index = polyLines.size
        polyLines.add(index = index, mutableListOf())
        distances.add(index = index, 0.0)
        durationPerRoute.add(index = index, ZERO)
        Log.d("TAG", "startTracking: current polyline size ${polyLines.size}")
        job = mainRepository.getLocation().onEach { response ->
            when (response) {
                is LocationResponse.Success -> {
                    response.data?.let { data ->
                        polyLines.last().add(LatLng(data.latitude, data.longitude))
                        distances.set(index = index, SphericalUtil.computeLength(polyLines.last()))

                        if (
                            lastLocation != null &&
                            LatLng(
                                lastLocation?.latitude ?: 0.0,
                                lastLocation?.longitude ?: 0.0
                            ) != LatLng(data.latitude, data.longitude)
                            ) {
                                movingTime = movingTime.plus(duration.minus(durationTemp))
                            } else {
                                Log.d("TAG", "isMoving: false")
                            }

                        if (distances.sum() == 0.0) {
                            pacePerKm.set(index = durationPerKm.size - 1, ZERO)
                        } else {
                            if (distances.sum().toInt() / 1000 > durationPerKm.size - 1) {
                                durationPerKm.add(index = durationPerKm.size, ZERO)
                                pacePerKm.add(index = durationPerKm.size - 1, ZERO)
                            } else {
                                pacePerKm.set(
                                    index = durationPerKm.size - 1,
                                    (durationPerKm.last().inWholeSeconds / ((distances.sum() / 1000.0) - (durationPerKm.size - 1))).toInt().seconds
                                )
                            }
                        }

                        durationTemp = duration
                        lastLocation = data

                        if (data.hasSpeed()) {
                            speeds.add(data.speed * 3.6f)
                        }

                        durationPerRoute.set(
                            index = durationPerRoute.size-1,
                            durationPerRoute.last().plus(
                                duration.minus(
                                    durationPerRoute.sumOf { it.inWholeSeconds }.toDuration(DurationUnit.SECONDS)
                                )
                            )
                        )

                        durationPerKm.set(
                            index = durationPerKm.size-1,
                            durationPerKm.last().plus(
                                duration.minus(
                                    durationPerKm.sumOf { it.inWholeSeconds }.toDuration(DurationUnit.SECONDS)
                                )
                            )
                        )

                        _serviceData.update { serviceData ->
                            serviceData.copy(
                                elapsedTimePerKm = durationPerKm,
                                elapsedTimePerRoute = durationPerRoute,
                                movingTime = movingTime,
                                pacePerKm = pacePerKm,
                                routes = polyLines,
                                distancePerRoute = distances,
                                lastLocation = lastLocation,
                                speeds = speeds,
                                exception = exception
                            )
                        }
                    }
                }
                is LocationResponse.Error -> {
                    _serviceData.update { it.copy(exception = response.exception) }
                    cancel()
                }
            }
        }.launchIn(serviceScope)

        timer = fixedRateTimer(initialDelay = 1000, period = 1000) {
            duration = duration.plus(1.seconds)
            _serviceData.update { serviceData -> serviceData.copy(duration = duration) }
            onChange(duration, distances)
        }
    }

    private fun stopTracking() {
        if (job != null) {
            job?.cancel()
            job = null
        }
        if (this::timer.isInitialized) {
            timer.cancel()
        }
    }

    private fun resetAllData() {
        distances.clear()
        polyLines.clear()
        speeds.clear()

        duration = ZERO
        durationTemp = ZERO
        movingTime = ZERO

        durationPerKm = mutableListOf(ZERO)
        durationPerRoute = mutableListOf(ZERO)
        pacePerKm = mutableListOf(ZERO)

        lastLocation = null

        _serviceData.update { ServiceData() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(duration: Duration, distances: MutableList<Double>) {
        notificationManager.notify(
            NOTIFICATION_ID,
            notificationBuilder
                .setContentText(duration.durationToTimerFormat())
                .setContentTitle(distances.sum().formatDistance())
                .build()
        )
    }

    private fun setNotificationButton() {
        notificationBuilder.clearActions()
        when (serviceData.value.state) {
            LocationTrackingState.Started -> {
                notificationBuilder
                    .addAction(0, getString(R.string.btn_pause), ServiceHelper.pausePendingIntent(this))
                    .addAction(0, getString(R.string.btn_cancel), ServiceHelper.cancelPendingIntent(this))
                    .addAction(0, getString(R.string.btn_finish), ServiceHelper.finishPendingIntent(this))
            }
            LocationTrackingState.Paused -> {
                notificationBuilder
                    .addAction(0, getString(R.string.btn_resume), ServiceHelper.resumePendingIntent(this))
                    .addAction(0, getString(R.string.btn_cancel), ServiceHelper.cancelPendingIntent(this))
                    .addAction(0, getString(R.string.btn_finish), ServiceHelper.finishPendingIntent(this))
            }
            else -> {}
        }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}