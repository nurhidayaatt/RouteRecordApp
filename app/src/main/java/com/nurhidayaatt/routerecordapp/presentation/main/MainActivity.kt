package com.nurhidayaatt.routerecordapp.presentation.main

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import com.nurhidayaatt.routerecordapp.R
import com.nurhidayaatt.routerecordapp.data.LocationResponse
import com.nurhidayaatt.routerecordapp.databinding.ActivityMainBinding
import com.nurhidayaatt.routerecordapp.domain.model.Routes
import com.nurhidayaatt.routerecordapp.presentation.SharedViewModel
import com.nurhidayaatt.routerecordapp.presentation.routes_data.TabPagerAdapter
import com.nurhidayaatt.routerecordapp.services.LocationService
import com.nurhidayaatt.routerecordapp.services.LocationTrackingState
import com.nurhidayaatt.routerecordapp.services.ServiceHelper
import com.nurhidayaatt.routerecordapp.util.*
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_CANCEL
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_FINISH
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_PAUSE
import com.nurhidayaatt.routerecordapp.util.Constants.ACTION_SERVICE_START
import com.nurhidayaatt.routerecordapp.util.Constants.CANCEL_SERVICE_DELAY
import com.nurhidayaatt.routerecordapp.util.Constants.DEFAULT_DELAY
import com.nurhidayaatt.routerecordapp.util.Constants.SNAPSHOT_DELAY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.internal.wait
import java.time.LocalDateTime
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()
    private val sharedViewModel by viewModels<SharedViewModel>()
    private lateinit var binding: ActivityMainBinding
    private var map: GoogleMap? = null
    private var mapSnapshot: GoogleMap? = null

    private var activityType: String? = null

    private lateinit var locationService: LocationService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()
            viewModel.updateBoundServices(isConnected = true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.updateBoundServices(isConnected = false)
        }
    }

    private var polyLines: MutableList<Polyline?> = mutableListOf()
    private var polyLinesDetail: MutableList<Polyline?> = mutableListOf()

    private lateinit var carouselPagerAdapter: CarouselPagerAdapter
    private val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            sharedViewModel.updatePosition(position)
            super.onPageSelected(position)
        }
    }

    private lateinit var mainBottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var routeDataBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var trackingBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var mapSnapshotBehavior: BottomSheetBehavior<ConstraintLayout>

    private val tabTitle = listOf("Routes", "Statistics")
    private val tabIcon = listOf(R.drawable.ic_route, R.drawable.ic_assessment)
    private val locationPermissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
    @RequiresApi(TIRAMISU)
    private val notificationPerm = arrayOf(POST_NOTIFICATIONS)
    @RequiresApi(Q)
    private val backgroundLocationPerm = arrayOf(ACCESS_BACKGROUND_LOCATION)

    private val gpsLauncherGetCurrentLocation = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.showMyLocation()
        }
    }

    private val gpsLauncherService = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startTracking()
        }
    }

    @SuppressLint("NewApi")
    private val mapPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result.entries.forEach { entries ->
            when (entries.key) {
                locationPermissions[0], locationPermissions[1] -> {
                    viewModel.updateLocationPermission(isGranted = entries.value).invokeOnCompletion {
                        if (entries.value) {
                            viewModel.showMyLocation()
                        }
                    }
                }
                backgroundLocationPerm[0] -> {
                    viewModel.updateBackgroundLocationPermission(isGranted = entries.value)
                }
                notificationPerm[0] -> {
                    viewModel.updateNotificationPermission(isGranted = entries.value)
                    if (entries.value) { backgroundLocationLauncher.launch(backgroundLocationPerm[0]) }
                }
            }
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.updateBackgroundLocationPermission(isGranted = it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleIntent(intent = intent)
        handleWindowInset()
        initView()
        handleState()
        initBottomSheet()
        initCarouselLayout()
        handleBackStack()
    }

    private fun initView() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap

            map?.let { map ->
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.simple_map_style))
                map.uiSettings.apply {
                    isCompassEnabled = false
                    isMapToolbarEnabled = false
                    isMyLocationButtonEnabled = false
                }
            }
        }

        binding.apply {
            fabMyLocation.setOnClickListener {
                viewModel.showMyLocation()
            }

            layoutBottomSheetMain.apply {
                toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
                    when(checkedId) {
                        R.id.btn_running -> {
                            if (isChecked) {
                                viewModel.updateUserPref(activityType = ActivityType.RUNNING)
                            }
                        }
                        R.id.btn_cycling -> {
                            if (isChecked) {
                                viewModel.updateUserPref(activityType = ActivityType.CYCLING)
                            }
                        }
                    }
                }

                btnStart.setOnClickListener {
                    startTracking()
                }
                btnDataRoutes.setOnClickListener {
                    viewModel.showDataRoutes(show = true)
                }
            }

            layoutTracking.apply {
                btnCancel.setOnClickListener {
                    viewModel.updateLocationServiceState(state = LocationTrackingState.Canceled)
                }
                btnFinish.setOnClickListener {
                    viewModel.updateLocationServiceState(state = LocationTrackingState.Finished)
                }
            }
        }
    }

    private fun startTracking() {
        when {
            SDK_INT < Q -> {
                viewModel.updateLocationServiceState(state = LocationTrackingState.Started)
            }
            SDK_INT in Q until TIRAMISU -> {
                mapPermissionsLauncher.launch(backgroundLocationPerm)
            }
            SDK_INT >= TIRAMISU -> {
                mapPermissionsLauncher.launch(notificationPerm)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isServicesAvailable.collectLatest {
                        handleGoogleServicesState(available = it)
                    }
                }

                launch {
                    viewModel.permissionState.collectLatest { permState ->
                        handlePermissionAndGpsAlert(state = permState)
                    }
                }

                launch {
                    viewModel.myLocationEnabled.collectLatest {
                        if (map == null) {
                            delay(DEFAULT_DELAY)
                        }
                        map?.isMyLocationEnabled = it
                    }
                }

                launch {
                    viewModel.currentLocationState.collectLatest {
                        when (it) {
                            is LocationResponse.Success -> {
                                map?.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(it.data!!.latitude, it.data.longitude), 16f
                                    )
                                )
                            }
                            is LocationResponse.Error -> {
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(it.exception!!.resolution).build()
                                gpsLauncherGetCurrentLocation.launch(intentSenderRequest)
                            }
                        }
                    }
                }

                launch {
                    viewModel.uiState.collectLatest { uiState ->
                        if (!uiState.showDataRoutes || uiState.showRoutePreview) {
                            routeDataBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        } else {
                            routeDataBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            map?.setPadding(0, 0, 0, 0)
                        }

                        if (uiState.showRoutePreview) {
                            binding.viewPager.visibility = View.VISIBLE
                        } else {
                            binding.viewPager.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.userPref.collectLatest {
                        when(it) {
                            ActivityType.RUNNING -> {
                                binding.layoutBottomSheetMain.toggleButton.check(R.id.btn_running)
                                activityType = ActivityType.RUNNING.name
                            }
                            ActivityType.CYCLING -> {
                                binding.layoutBottomSheetMain.toggleButton.check(R.id.btn_cycling)
                                activityType = ActivityType.CYCLING.name
                            }
                        }
                    }
                }

                launch {
                    viewModel.showMainBottomSheet.collectLatest {
                        if (it) {
                            mainBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            delay(DEFAULT_DELAY)
                            map?.setPadding(0, 0, 0, binding.layoutBottomSheetMain.root.height)
                        } else {
                            mainBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }
                }

                launch {
                    viewModel.hideFab.collectLatest {
                        if (it) {
                            binding.fabMyLocation.hide()
                        } else {
                            binding.fabMyLocation.show()
                        }
                    }
                }

                launch {
                    viewModel.boundServiceConnected.collect { isBound ->
                        if (isBound) {
                            launch {
                                locationService.lastState.collectLatest {
                                    viewModel.updateLastStateActiveService(state = it)
                                }
                            }

                            launch {
                                locationService.serviceData.collect { serviceData ->
                                    binding.layoutTracking.tvTime.text =
                                        serviceData.duration.durationToTimerFormat()

                                    binding.layoutTracking.tvDistance.text =
                                        serviceData.distancePerRoute.formatDistance()

                                    binding.layoutTracking.tvAvgPace.text =
                                        serviceData.pacePerKm.last().formatPace()

                                    /*binding.layoutTracking.tvTimeTitle.text =
                                        serviceData.movingTime.durationToString(this@MainActivity)*/

                                    if (serviceData.routes.isNotEmpty()) {
                                        serviceData.routes.forEach {
                                            polyLines.add(map?.addPolyline(
                                                PolylineOptions().color(colorPolyLine).addAll(it))
                                            )
                                            if (polyLines.size > serviceData.routes.size) {
                                                polyLines[serviceData.routes.size - 1]?.remove()
                                                polyLines.removeAt(serviceData.routes.size - 1)
                                            }
                                        }
                                    }

                                    // only trigger first time on ui and not trigger when service already running
                                    serviceData.exception?.let {
                                        val intentSenderRequest =
                                            IntentSenderRequest.Builder(it.resolution).build()
                                        gpsLauncherService.launch(intentSenderRequest)
                                    }

                                    showLayoutTracking(
                                        state = serviceData.state != LocationTrackingState.Idle
                                    )

                                    serviceData.lastLocation?.let { location ->
                                        map?.animateCamera(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition(
                                                    LatLng(location.latitude, location.longitude),
                                                    18f,
                                                    0f,
                                                    location.bearing
                                                )
                                            )
                                        )
                                    }

                                    if (serviceData.state != LocationTrackingState.Finished) {
                                        viewModel.updateLocationServiceState(
                                            state = serviceData.state
                                        )
                                    }

                                    when (serviceData.state) {
                                        LocationTrackingState.Idle -> {
                                            polyLines.forEach { it?.remove() }
                                            polyLines.clear()
                                        }
                                        LocationTrackingState.Started -> {
                                            binding.apply {
                                                layoutTracking.btnStartPause.text = getString(R.string.btn_pause)
                                                layoutTracking.btnStartPause.setOnClickListener {
                                                    viewModel.updateLocationServiceState(
                                                        state = LocationTrackingState.Paused
                                                    )
                                                }
                                            }
                                        }
                                        LocationTrackingState.Paused -> {
                                            binding.apply {
                                                layoutTracking.btnStartPause.text = getString(R.string.btn_resume)
                                                binding.layoutTracking.btnStartPause.setOnClickListener {
                                                    viewModel.updateLocationServiceState(
                                                        state = LocationTrackingState.Started
                                                    )
                                                }
                                            }
                                        }
                                        LocationTrackingState.Finished -> {
                                            showLoading(state = true)
                                            polyLines.forEach { it?.remove() }
                                            polyLines.clear()
                                            val builder = LatLngBounds.Builder()
                                            serviceData.routes.forEach {
                                                polyLines.add(mapSnapshot?.addPolyline(
                                                    PolylineOptions().color(colorPolyLine).addAll(it)
                                                ))
                                                it.forEach { latlng ->
                                                    builder.include(latlng)
                                                }
                                            }
                                            mapSnapshot?.moveCamera(
                                                CameraUpdateFactory.newLatLngBounds(builder.build(), 100),
                                            )

                                            delay(timeMillis = SNAPSHOT_DELAY)
                                            mapSnapshot?.snapshot { bmp ->
                                                val route = Routes(
                                                    typeActivity = activityType,
                                                    timestamp = LocalDateTime.now(),
                                                    img = bmp,
                                                    elapsedTime = serviceData.elapsedTimePerKm.sumOf { time -> time.inWholeSeconds },
                                                    elapsedTimePerKm = serviceData.elapsedTimePerKm.map { time -> time.inWholeSeconds },
                                                    elapsedTimePerRoute = serviceData.elapsedTimePerRoute.map { time -> time.inWholeSeconds },
                                                    movingTime = serviceData.movingTime.inWholeSeconds,
                                                    pacePerKm = serviceData.pacePerKm.map { time -> time.inWholeSeconds },
                                                    routes = serviceData.routes,
                                                    distance = serviceData.distancePerRoute.sum(),
                                                    distancePerRoute = serviceData.distancePerRoute
                                                )
                                                viewModel.saveData(routes = route).invokeOnCompletion {
                                                    ServiceHelper.triggerForegroundService(
                                                        context = this@MainActivity,
                                                        ACTION_SERVICE_CANCEL
                                                    )
                                                    showLoading(state = false)
                                                    viewModel.showDataRoutes(show = true)
                                                    binding.layoutRouteData.tabLayout.getTabAt(0)
                                                        ?.select()
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }

                // TODO: Extract string resource
                launch {
                    viewModel.serviceState.collect { serviceState ->
                        when (serviceState.currentState) {
                            LocationTrackingState.Started -> {
                                ServiceHelper.triggerForegroundService(
                                    context = this@MainActivity,
                                    ACTION_SERVICE_START
                                )
                            }
                            LocationTrackingState.Paused -> {
                                ServiceHelper.triggerForegroundService(
                                    context = this@MainActivity,
                                    ACTION_SERVICE_PAUSE
                                )
                            }
                            LocationTrackingState.Canceled -> {
                                showAlertDialog(
                                    context = this@MainActivity,
                                    icon = R.drawable.ic_route,
                                    title = "Cancel Tracking",
                                    message = "Do you really want cancel tacking?\nroute data will deleted and not save to database",
                                    positiveButtonText = "Yes",
                                    onPositiveClick = {
                                        ServiceHelper.triggerForegroundService(
                                            context = this@MainActivity,
                                            ACTION_SERVICE_CANCEL
                                        )
                                    }
                                )
                            }
                            LocationTrackingState.Finished -> {
                                showAlertDialog(
                                    context = this@MainActivity,
                                    icon = R.drawable.ic_route,
                                    title = "Finish Tracking",
                                    message = "Do you really want finish tacking and save route?",
                                    positiveButtonText = "Yes",
                                    onPositiveClick = {
                                        ServiceHelper.triggerForegroundService(
                                            context = this@MainActivity,
                                            ACTION_SERVICE_FINISH
                                        )
                                    }
                                )
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    sharedViewModel.route.collect { data ->
                        if (!data.routes.isNullOrEmpty() && data.position != null) {
                            carouselPagerAdapter.differ.submitList(data.routes)
                            viewModel.showRoutePreview(show = true)

                            if (map == null || data.needDelay) {
                                delay(DEFAULT_DELAY)
                            }

                            binding.viewPager .currentItem = data.position
                            map?.setPadding(0, 0, 0, binding.layoutRoutePreview.height)

                            polyLinesDetail.forEach { it?.remove() }
                            polyLinesDetail.clear()

                            val builder = LatLngBounds.Builder()
                            data.routes[data.position].routes.forEach {
                                polyLinesDetail.add(
                                    map?.addPolyline(
                                        PolylineOptions().color(colorPolyLine).addAll(it)
                                    )
                                )
                                it.forEach { latlng ->
                                    builder.include(latlng)
                                }
                            }

                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(builder.build(), 100)
                            )
                        } else {
                            viewModel.showRoutePreview(show = false)
                            carouselPagerAdapter.differ.submitList(null)
                            polyLinesDetail.forEach { it?.remove() }
                            polyLinesDetail.clear()
                        }
                    }
                }
            }
        }
    }

    private fun handleGoogleServicesState(available: Boolean) {
        if (!available) {
            binding.fabMyLocation.visibility = View.GONE
        } else {
            mapPermissionsLauncher.launch(locationPermissions)
        }
    }

    private fun handlePermissionAndGpsAlert(state: PermissionState) {
        if (!state.showAlertDialog) return

        val appDetailIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", this.packageName, null)
        )

        if (!state.locationPermissionGranted) {
            showAlertDialog(
                context = this,
                icon = R.drawable.ic_location_off,
                title = String.format(
                    getString(R.string.dialog_title),
                    getString(R.string.location)
                ),
                message = if (!shouldShowRequestPermissionRationale(locationPermissions[0])) {
                    String.format(
                        getString(R.string.dialog_message_permission_permanently_decline),
                        getString(R.string.location)
                    )
                } else {
                    String.format(getString(R.string.dialog_message), getString(R.string.location))
                       },
                positiveButtonText = if (
                    !shouldShowRequestPermissionRationale(locationPermissions[0])
                ) {
                    getString(R.string.dialog_positive_button_grant_permission)
                } else { getString(R.string.dialog_positive_button_default) },
                onPositiveClick = {
                    if (!shouldShowRequestPermissionRationale(locationPermissions[0])) {
                        startActivity(appDetailIntent)
                    } else { mapPermissionsLauncher.launch(locationPermissions) }
                                  },
                onDismiss = { viewModel.showAlertDialog(show = false) }
            )
        } else if (!state.postNotificationPermissionGranted) {
            if (SDK_INT < TIRAMISU) return
            val permanentlyDenied = !shouldShowRequestPermissionRationale(notificationPerm[0])

            showAlertDialog(
                context = this,
                icon = R.drawable.ic_notifications_off,
                title = String.format(
                    getString(R.string.dialog_title),
                    getString(R.string.post_notification)
                ),
                message = if (permanentlyDenied) {
                    String.format(
                        getString(R.string.dialog_message_permission_permanently_decline),
                        getString(R.string.post_notification)
                    )
                } else {
                    String.format(
                        getString(R.string.dialog_message),
                        getString(R.string.post_notification)
                    )
                },
                positiveButtonText = if (permanentlyDenied) {
                    getString(R.string.dialog_positive_button_grant_permission)
                } else { getString(R.string.dialog_positive_button_default) },
                onPositiveClick = {
                    if (permanentlyDenied) {
                        startActivity(appDetailIntent)
                    } else {
                        mapPermissionsLauncher.launch(notificationPerm)
                    }
                },
                onDismiss = { viewModel.showAlertDialog(show = false) }
            )
        } else if (!state.backgroundLocationPermissionGranted) {
            if (SDK_INT < Q) return
            val permanentlyDenied = !shouldShowRequestPermissionRationale(backgroundLocationPerm[0])

            showAlertDialog(
                context = this,
                icon = R.drawable.ic_location_off,
                title = String.format(
                    getString(R.string.dialog_title),
                    getString(R.string.background_location)
                ),
                message = if (permanentlyDenied) {
                    getString(
                        R.string.dialog_message_background_location_permission_permanently_decline
                    )
                } else {
                    String.format(
                        getString(R.string.dialog_message),
                        getString(R.string.background_location)
                    )
                       },
                positiveButtonText = if (permanentlyDenied) {
                    getString(R.string.dialog_positive_button_grant_permission)
                } else { getString(R.string.dialog_positive_button_default) },
                onPositiveClick = {
                    if (permanentlyDenied) {
                        startActivity(appDetailIntent)
                    } else {
                        mapPermissionsLauncher.launch(backgroundLocationPerm)
                    }
                },
                onDismiss = { viewModel.showAlertDialog(show = false) }
            )
        }
    }

    private fun initBottomSheet() {
        mainBottomSheetBehavior = BottomSheetBehavior.from(binding.layoutBottomSheetMain.root)

        // Layout Tracking
        trackingBehavior = BottomSheetBehavior.from(binding.layoutTracking.root)

        // Layout Route Data
        val layoutTrackingData = binding.layoutRouteData
        routeDataBehavior = BottomSheetBehavior.from(layoutTrackingData.root)

        val adapter = TabPagerAdapter(supportFragmentManager, lifecycle)
        layoutTrackingData.tabPager.adapter = adapter
        TabLayoutMediator(layoutTrackingData.tabLayout, layoutTrackingData.tabPager) { tab, position ->
            tab.text = tabTitle[position]
            tab.icon = AppCompatResources.getDrawable(this, tabIcon[position])
        }.attach()

        // Layout Map Snapshot
        mapSnapshotBehavior = BottomSheetBehavior.from(binding.layoutMapSnapshot.root)
        mapSnapshotBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val mapSnapshotFragment = supportFragmentManager.findFragmentById(R.id.map_snapshot) as SupportMapFragment

        mapSnapshotFragment.getMapAsync { map ->
            mapSnapshot = map
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.snapshot_map_style))
            map.uiSettings.apply {
                isCompassEnabled = false
                isMyLocationButtonEnabled = false
                isMapToolbarEnabled = false
                isZoomControlsEnabled = false
                setAllGesturesEnabled(false)
            }
        }
    }

    private fun initCarouselLayout() {
        carouselPagerAdapter = CarouselPagerAdapter(context = this)
        val pager = binding.viewPager
        pager.adapter = carouselPagerAdapter
        pager.clipChildren = false
        pager.clipToPadding = false
        pager.offscreenPageLimit = 3
        (pager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer((8 * resources.displayMetrics.density).toInt()))
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = (0.80f + r * 0.20f)
        }
        pager.setPageTransformer(compositePageTransformer)

        binding.viewPager.registerOnPageChangeCallback(callback)
    }

    suspend fun updateImageRoute(data: Routes) {
        showLoading(state = true)
        polyLines.forEach { it?.remove() }
        polyLines.clear()
        val builder = LatLngBounds.Builder()
        data.routes.forEach {
            polyLines.add(mapSnapshot?.addPolyline(
                PolylineOptions().color(colorPolyLine).addAll(it)
            ))
            it.forEach { latlng ->
                builder.include(latlng)
            }
        }
        mapSnapshot?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(builder.build(), 100),
        )

        delay(timeMillis = SNAPSHOT_DELAY)
        mapSnapshot?.snapshot { bmp ->
            bmp?.let {
                viewModel.updateImage(id = data.id!!, img = it)
            }
            showLoading(false)
        }
    }

    private fun showLayoutTracking(state: Boolean) {
        if (state) {
            trackingBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            map?.setPadding(0, 0, 0, binding.layoutTracking.root.height)
        } else {
            trackingBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun showLoading(state: Boolean) {
        if (state) {
            binding.layoutLoading.layoutLoading.visibility = View.VISIBLE
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        } else {
            binding.layoutLoading.layoutLoading.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    private fun handleWindowInset() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val parentParams = view.layoutParams as ViewGroup.MarginLayoutParams
            parentParams.updateMargins(
                left = insets.left,
                top = if (SDK_INT >= Q) 0 else insets.top,
                right = insets.right,
                bottom = insets.bottom
            )

            val layoutTrackingDataParams =
                binding.layoutRouteData.tabLayout.layoutParams as ViewGroup.MarginLayoutParams
            layoutTrackingDataParams.updateMargins(
                top = if (SDK_INT >= Q) insets.top else 0
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun handleBackStack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    routeDataBehavior.state != BottomSheetBehavior.STATE_HIDDEN -> {
                        viewModel.showDataRoutes(show = false)
                    }
                    trackingBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> {
                        // TODO: do something about foreground service
                        trackingBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                    polyLinesDetail.size > 0 -> {
                        viewModel.showDataRoutes(show = true)
                        sharedViewModel.setRoute()
                    }
                    mainBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> finish()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent=  intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.getStringExtra(Constants.LOCATION_TRACKING_STATE)) {
            LocationTrackingState.Finished.name -> viewModel.updateLocationServiceState(
                state = LocationTrackingState.Finished
            )
            LocationTrackingState.Canceled.name -> viewModel.updateLocationServiceState(
                state = LocationTrackingState.Canceled
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, LocationService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        viewModel.updateBoundServices(isConnected = false)
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(callback)
        super.onDestroy()
    }
}