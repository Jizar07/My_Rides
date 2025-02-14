package com.example.myrides.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import com.example.myrides.R
import com.example.myrides.places.fetchNearbyPOIs
import com.example.myrides.places.PlaceResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Marker
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

// ------------------------------
// Helper Composable to Listen for Live Location Updates
// ------------------------------
fun smoothLocation(newLocation: LatLng, oldLocation: LatLng?, smoothingFactor: Float = 0.1f): LatLng {
    // If no previous location, return new location directly.
    if (oldLocation == null) return newLocation
    val lat = oldLocation.latitude + smoothingFactor * (newLocation.latitude - oldLocation.latitude)
    val lng = oldLocation.longitude + smoothingFactor * (newLocation.longitude - oldLocation.longitude)
    return LatLng(lat, lng)
}

@Composable
fun currentLocationViaCallback(): LatLng? {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationState = remember { mutableStateOf<LatLng?>(null) }

    DisposableEffect(Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { loc ->
                    // Only process updates if the location accuracy is acceptable (e.g., <= 50 meters)
                    if (loc.accuracy <= 50) {
                        val newLocation = LatLng(loc.latitude, loc.longitude)
                        val oldLocation = locationState.value
                        // Define a smoothing factor (0.1 means 10% of the new value and 90% of the old value)
                        val smoothingFactor = 0.1f
                        val smoothedLocation = if (oldLocation == null) {
                            newLocation
                        } else {
                            LatLng(
                                oldLocation.latitude * (1 - smoothingFactor) + newLocation.latitude * smoothingFactor,
                                oldLocation.longitude * (1 - smoothingFactor) + newLocation.longitude * smoothingFactor
                            )
                        }
                        locationState.value = smoothedLocation
                    } else {
                        Log.d("LocationUpdates", "Update ignored due to poor accuracy: ${loc.accuracy}")
                    }
                }
            }
        }


        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (se: SecurityException) {
                Log.e("LocationUpdates", "Security exception: ${se.message}")
            }
        } else {
            Log.w("LocationUpdates", "ACCESS_FINE_LOCATION not granted!")
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    return locationState.value
}


// ------------------------------
// Date Range Helper Function (Local Offset Format)
// ------------------------------
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentWeekRange(): Pair<String, String> {
    val zone = ZoneId.of("America/New_York")
    val today = LocalDate.now(zone)
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val nextMonday = monday.plusDays(7)
    val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    val startDateTime = monday.atStartOfDay(zone).format(formatter)
    val endDateTime = nextMonday.atStartOfDay(zone).format(formatter)
    Log.d("DateRange", "Start: $startDateTime, End: $endDateTime")
    return Pair(startDateTime, endDateTime)
}

// ------------------------------
// Data Class for Social Event
// ------------------------------
data class SocialEvent(
    val name: String,
    val description: String,
    val location: LatLng,
    val startTime: String,   // e.g., "7:00 PM"
    val date: String,        // e.g., "2025-02-12"
    val venueName: String,
    val address: String,
    val organizer: String,
    val ticketPrice: Double? // null if free
)

// ------------------------------
// Retrofit Service Interface for Ticketmaster API
// ------------------------------
interface TicketmasterService {
    @GET("discovery/v2/events.json")
    suspend fun getEvents(
        @Query("apikey") apiKey: String,
        @Query("latlong") latlong: String,
        @Query("radius") radius: Int,
        @Query("unit") unit: String = "miles",
        @Query("startDateTime") startDateTime: String,
        @Query("endDateTime") endDateTime: String,
        @Query("countryCode") countryCode: String = "US",
        @Query("size") size: Int = 20
    ): TicketmasterResponse

    companion object {
        fun create(): TicketmasterService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://app.ticketmaster.com/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            return retrofit.create(TicketmasterService::class.java)
        }
    }
}

// ------------------------------
// Data Classes for API Response
// ------------------------------
data class TicketmasterResponse(
    val _embedded: EmbeddedEvents?
)

data class EmbeddedEvents(
    val events: List<TicketmasterEvent>?
)

data class TicketmasterEvent(
    val name: String,
    val dates: Dates,
    val info: String?,
    val _embedded: EmbeddedVenues?
)

data class Dates(
    val start: Start
)

data class Start(
    val localTime: String?,
    val localDate: String?
)

data class EmbeddedVenues(
    val venues: List<Venue>?
)

data class Venue(
    val name: String?,
    val address: Address?,
    val city: City?,
    val location: LocationInfo
)

data class Address(
    val line1: String?
)

data class City(
    val name: String?
)

data class LocationInfo(
    val latitude: String?,
    val longitude: String?
)

// ------------------------------
// Function to Fetch Social Events (Real API Integration)
// ------------------------------
@RequiresApi(Build.VERSION_CODES.O)
suspend fun fetchSocialEvents(currentLocation: LatLng, radiusMiles: Double): List<SocialEvent> {
    return withContext(Dispatchers.IO) {
        try {
            val service = TicketmasterService.create()
            val (startDateTime, endDateTime) = getCurrentWeekRange()
            Log.d("Ticketmaster", "Fetching events with startDateTime: $startDateTime, endDateTime: $endDateTime")
            val response = service.getEvents(
                apiKey = "vGsATDpC11wAdA15Bj6qgBoYs34ZBpeE",
                latlong = "${currentLocation.latitude},${currentLocation.longitude}",
                radius = radiusMiles.toInt(),
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            Log.d("Ticketmaster", "Raw response: $response")
            val events = response._embedded?.events ?: emptyList()
            events.mapNotNull { event ->
                val venue = event._embedded?.venues?.firstOrNull()
                val lat = venue?.location?.latitude?.toDoubleOrNull()
                val lng = venue?.location?.longitude?.toDoubleOrNull()
                if (venue != null && lat != null && lng != null) {
                    SocialEvent(
                        name = event.name,
                        description = event.info ?: "No additional info",
                        location = LatLng(lat, lng),
                        startTime = event.dates.start.localTime ?: "",
                        date = event.dates.start.localDate ?: "",
                        venueName = venue.name ?: "",
                        address = "${venue.address?.line1 ?: ""}, ${venue.city?.name ?: ""}",
                        organizer = "Ticketmaster",
                        ticketPrice = null
                    )
                } else null
            }
        } catch (e: HttpException) {
            Log.e("Ticketmaster", "HTTP error: ${e.code()} - ${e.response()?.errorBody()?.string()}", e)
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            Log.e("Ticketmaster", "Error fetching events", e)
            e.printStackTrace()
            emptyList()
        }
    }
}

// ------------------------------
// Helper Function to Fetch Last Known Location (One-Time Fetch)
// ------------------------------
@SuppressLint("MissingPermission")
private suspend fun getLastKnownLocation(
    fusedLocationClient: FusedLocationProviderClient
): Location? = withContext(Dispatchers.IO) {
    try {
        fusedLocationClient.lastLocation.await()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ------------------------------
// Dynamic Scaling for Marker Icon (Approach 1)
// ------------------------------

// Load the original bitmap for the marker.
//@Composable
fun scaledMarkerIcon(context: Context, cameraZoom: Float): BitmapDescriptor {
    val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.car)
    val baseZoom = 20f
    val scaleFactor = (cameraZoom / baseZoom).coerceAtLeast(0.05f)
    val newWidth = (originalBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
    val newHeight = (originalBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false)
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}

fun scaledSocialEventIconToday(context: Context, zoom: Float): BitmapDescriptor {
    // Load the original social event marker bitmap.
    // (Make sure you have an icon in your drawable folder named event_marker.png)
    val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.today)
    val baseZoom = 25f  // Define a base zoom level; adjust as needed.
    val scaleFactor = (zoom / baseZoom).coerceAtLeast(0.5f)
    val newWidth = (originalBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
    val newHeight = (originalBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false)
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}

fun scaledSocialEventIcon(context: Context, zoom: Float): BitmapDescriptor {
    // Load the original social event marker bitmap.
    // (Make sure you have an icon in your drawable folder named event_marker.png)
    val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.week)
    val baseZoom = 25f  // Define a base zoom level; adjust as needed.
    val scaleFactor = (zoom / baseZoom).coerceAtLeast(0.5f)
    val newWidth = (originalBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
    val newHeight = (originalBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false)
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}

fun scaledPOIIcon(context: Context, zoom: Float): BitmapDescriptor {
    // Load the original POI marker bitmap from your drawable folder.
    // Replace R.drawable.poi_icon with your actual drawable resource for POIs.
    val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.poi)
    val baseZoom = 25f  // This is your reference zoom level.
    val scaleFactor = (zoom / baseZoom).coerceAtLeast(0.5f)
    val newWidth = (originalBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
    val newHeight = (originalBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false)
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}


// ------------------------------
// Helper Functions for Clustering
// ------------------------------

fun distanceBetween(loc1: LatLng, loc2: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results)
    return results[0]
}

fun getClusterCentroid(cluster: List<PlaceResult>): LatLng {
    val avgLat = cluster.map { it.geometry.location.lat }.average()
    val avgLng = cluster.map { it.geometry.location.lng }.average()
    return LatLng(avgLat, avgLng)
}

fun clusterPOIs(pois: List<PlaceResult>, thresholdMeters: Float = 800f): List<List<PlaceResult>> {
    val clusters = mutableListOf<MutableList<PlaceResult>>()
    for (poi in pois) {
        var added = false
        for (cluster in clusters) {
            // Calculate the centroid of the current cluster.
            val centroid = getClusterCentroid(cluster)
            // If the distance between the current POI and the cluster centroid is less than the threshold, add it to the cluster.
            if (distanceBetween(LatLng(poi.geometry.location.lat, poi.geometry.location.lng), centroid) < thresholdMeters) {
                cluster.add(poi)
                added = true
                break
            }
        }
        if (!added) {
            // If the POI wasn't added to any cluster, create a new cluster.
            clusters.add(mutableListOf(poi))
        }
    }
    return clusters
}

// ------------------------------
// MapScreen Composable (Live GPS Tracking with Top Toggles, Recenter Button, Bottom Footer, and Dynamic Marker Scaling)
// ------------------------------
@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current

    // Initialize the Google Maps SDK.
    LaunchedEffect(Unit) {
        com.google.android.gms.maps.MapsInitializer.initialize(context)
    }
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Use our live tracking helper to get continuous location updates.
    val liveLocation = currentLocationViaCallback()

    // State for current heading (in degrees).
    var currentHeading by remember { mutableFloatStateOf(0f) }
    // Optionally, store previous location to compute bearing.
    var previousLocation by remember { mutableStateOf<LatLng?>(null) }

    // When liveLocation updates, compute heading based on previous location.
    LaunchedEffect(liveLocation) {
        liveLocation?.let { newLoc ->
            previousLocation?.let { prev ->
                val androidLocationPrev = Location("").apply {
                    latitude = prev.latitude
                    longitude = prev.longitude
                }
                val androidLocationNew = Location("").apply {
                    latitude = newLoc.latitude
                    longitude = newLoc.longitude
                }
                // Calculate bearing in degrees.
                currentHeading = androidLocationPrev.bearingTo(androidLocationNew)
            }
            previousLocation = newLoc
        }
    }
    // Default fixed position: Center of Florida.
    val defaultLatLng = LatLng(28.0, -82.0)
    // Camera position uses liveLocation if available.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(liveLocation ?: defaultLatLng, 10f)
    }

    val locationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var isDarkModeEnabled by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(true) }
    val darkMapStyle = try {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    val mapProperties = if (isDarkModeEnabled && darkMapStyle != null) {
        MapProperties(mapStyleOptions = darkMapStyle)
    } else {
        MapProperties()
    }

    var socialEvents by remember { mutableStateOf<List<SocialEvent>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<SocialEvent?>(null) }

    // New state variable for nearby POIs.
    var nearbyPOIs by remember { mutableStateOf<List<com.example.myrides.places.PlaceResult>>(emptyList()) }

    if (locationPermissionState.status is PermissionStatus.Granted) {
        var initialCentered by remember { mutableStateOf(false) }

        // New state variable to store the last location used for updating POIs.
        var lastPOIUpdateLocation by remember { mutableStateOf<LatLng?>(null) }

        // Define a distance threshold (in meters) for updating POIs.
        val updateThresholdMeters = 50f
        var lastCameraUpdateLocation by remember { mutableStateOf<LatLng?>(null) }
        val cameraUpdateThresholdMeters = 20f  // adjust threshold as needed

        LaunchedEffect(liveLocation) {
            liveLocation?.let { deviceLocation ->
                if (!initialCentered) {
                    // Center the camera only on the first update.
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(deviceLocation, 12f)
                    initialCentered = true
                }
                // Only update camera if we've moved more than the threshold.
                if (lastCameraUpdateLocation == null ||
                    distanceBetween(lastCameraUpdateLocation!!, deviceLocation) > cameraUpdateThresholdMeters) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(deviceLocation, 12f)
                    lastCameraUpdateLocation = deviceLocation
                }
                // Check if we should update POIs based on the movement threshold.
                if (lastPOIUpdateLocation == null ||
                    distanceBetween(lastPOIUpdateLocation!!, deviceLocation) > updateThresholdMeters) {

                    // Fetch social events using the device location.
                    socialEvents = fetchSocialEvents(deviceLocation, 100.0)

                    // Fetch nearby POIs (restaurants, bars, hotels) using the device location.
                    nearbyPOIs = fetchNearbyPOIs(deviceLocation, 20.0)

                    // Update the last POI update location.
                    lastPOIUpdateLocation = deviceLocation
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties
            ) {
                Marker(
                    state = MarkerState(position = defaultLatLng),
                    title = "Center of Florida"
                )
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                    Text("Grant Location Permission")
                }
            }
            return
        }
    }

    // Wait for current location to be fetched before loading the map.
    if (liveLocation == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Fetching current location...", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        // Create a coroutine scope for recentering button (if needed)
        val scope = rememberCoroutineScope()
        // Observe the current zoom level and update the scaled icon.
        val context = LocalContext.current
        var scaledIcon by remember { mutableStateOf(BitmapDescriptorFactory.fromResource(R.drawable.car)) }
        var lastZoomForIcon by remember { mutableStateOf(cameraPositionState.position.zoom) }

        LaunchedEffect(cameraPositionState.position.zoom) {
            snapshotFlow { cameraPositionState.position.zoom }
                .distinctUntilChanged()
                .collectLatest { zoom ->
                    if (kotlin.math.abs(zoom - lastZoomForIcon) > 0.5f) {
                        scaledIcon = scaledMarkerIcon(context, zoom)
                        lastZoomForIcon = zoom
                    }
                }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top overlay: Toggles and Recenter Button.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xAAFFFFFF))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkModeEnabled) Icons.Default.NightlightRound else Icons.Default.WbSunny,
                        contentDescription = if (isDarkModeEnabled) "Dark Mode" else "Light Mode"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isDarkModeEnabled,
                        onCheckedChange = { isDarkModeEnabled = it }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = if (showEvents) "Events On" else "Events Off"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showEvents,
                        onCheckedChange = { showEvents = it }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = {
                            liveLocation?.let { deviceLocation ->
                                scope.launch {
                                    val update = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.fromLatLngZoom(deviceLocation, 15f)
                                    )
                                    cameraPositionState.animate(update = update)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Recenter"
                        )
                    }
                }
            }
            // Map content.
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties
                ) {
                    // Draw "Your Location" marker using the scaledIcon.
                    liveLocation?.let { deviceLocation ->
                        Marker(
                            state = MarkerState(position = deviceLocation),
                            title = "Your Location",
                            icon = scaledIcon,
                            rotation = currentHeading
                        )
                    }
                    // Draw social event markers.
                    if (showEvents) {
                        // Obtain the current zoom level.
                        val currentZoom = cameraPositionState.position.zoom
                        val todayDate = LocalDate.now(ZoneId.of("America/New_York")).toString()
                        socialEvents.forEach { event ->
                            if (event.date == todayDate) {
                                val socialIcon = scaledSocialEventIconToday(context, currentZoom)
                                Marker(
                                    state = MarkerState(position = event.location),
                                    title = event.name,
                                    icon = socialIcon,
                                    onClick = {
                                        selectedEvent = event
                                        false
                                    }
                                )
                            } else {
                                val socialIcon = scaledSocialEventIcon(context, currentZoom)
                                Marker(
                                    state = MarkerState(position = event.location),
                                    title = event.name,
                                    icon = socialIcon,
                                    onClick = {
                                        selectedEvent = event
                                        false
                                    }
                                )
                            }
                        }
                    }

                    val currentZoom = cameraPositionState.position.zoom
                    val poiIcon = scaledPOIIcon(LocalContext.current, currentZoom)

                    // Draw nearby POI markers (restaurants, bars, hotels) with clustering.
                    val clusters = clusterPOIs(nearbyPOIs, thresholdMeters = 800f)
                    clusters.forEach { cluster ->
                        if (cluster.size >= 3) {
                            // If there are 3 or more POIs in the cluster, show a cluster marker.
                            val centroid = getClusterCentroid(cluster)
                            Marker(
                                state = MarkerState(position = centroid),
                                title = "Cluster: ${cluster.size} POIs",
                                icon = poiIcon,
                                onClick = { false }
                            )
                        } else {
                            // Otherwise, show individual markers.
                            cluster.forEach { poi ->
                                val poiLatLng =
                                    LatLng(poi.geometry.location.lat, poi.geometry.location.lng)
                                Marker(
                                    state = MarkerState(position = poiLatLng),
                                    title = poi.name,
                                    icon = poiIcon,
                                    onClick = { false }
                                )
                            }
                        }
                    }
                }
                }

                    // Draw nearby POI markers (restaurants, bars, hotels) in yellow.
//                    nearbyPOIs.forEach { poi ->
//                        val poiLatLng = LatLng(poi.geometry.location.lat, poi.geometry.location.lng)
//                        Marker(
//                            state = MarkerState(position = poiLatLng),
//                            title = poi.name,
//                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW),
//                            onClick = { false }
//                        )
//                    }
//                }
//            }

            // Footer overlay for future functions.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xAAEEEEEE))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Footer: Future Functions")
            }
            // Overlay info window for a selected event.
            selectedEvent?.let { event ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .clickable { selectedEvent = null }
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = event.name, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Date: ${event.date} at ${event.startTime}", color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Venue: ${event.venueName}", color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Address: ${event.address}", color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Organizer: ${event.organizer}", color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = if (event.ticketPrice != null) "Ticket Price: $${event.ticketPrice}" else "Free Entry", color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = event.description, color = Color.Black)
                    }
                }
            }
        }
    }
}
