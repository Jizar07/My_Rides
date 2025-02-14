package com.example.myrides.places

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

// Replace with your actual Google Places API key.
private const val GOOGLE_PLACES_API_KEY = "AIzaSyAFTdeAWhbl50mz3SGssqhzVCpOzl1Rjg4"

/**
 * Fetches nearby POIs (restaurants, bars, and hotels) from the Google Places API.
 *
 * @param currentLocation The current location as a LatLng.
 * @param radius The search radius in meters (default is 1000).
 * @return A list of PlaceResult objects.
 */
suspend fun fetchNearbyPOIs(currentLocation: LatLng, radiusMiles: Double): List<PlaceResult> {
    return withContext(Dispatchers.IO) {
        try {
            val service = GooglePlacesService.create()
            // Construct the location string in "lat,lng" format.
            val radiusMeters = (radiusMiles * 1609.34).toInt()
            val locationStr = "${currentLocation.latitude},${currentLocation.longitude}"
            // Use a keyword that includes restaurants, bars, and hotels.
            val keyword = "restaurant|bar|lodging|night_club|pub"
            val response = service.getNearbyPlaces(
                location = locationStr,
                radius = radiusMeters,
                keyword = keyword,
                key = GOOGLE_PLACES_API_KEY
            )
            response.results ?: emptyList()
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error fetching nearby POIs", e)
            emptyList()
        }
    }
}
