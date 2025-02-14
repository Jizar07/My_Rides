package com.example.myrides.places

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// Data classes for parsing the Places API response.
data class PlacesResponse(
    val results: List<PlaceResult>?
)

data class PlaceResult(
    val name: String,
    val geometry: Geometry,
    val vicinity: String,
    val types: List<String>
)

data class Geometry(
    val location: PlaceLocation
)

data class PlaceLocation(
    val lat: Double,
    val lng: Double
)

interface GooglePlacesService {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String, // Format: "lat,lng"
        @Query("radius") radius: Int,          // Radius in meters
        @Query("keyword") keyword: String,     // e.g., "restaurant|bar|lodging"
        @Query("key") key: String              // Your Google Places API key
    ): PlacesResponse

    companion object {
        fun create(): GooglePlacesService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/") // Base URL for the Google Places API
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            return retrofit.create(GooglePlacesService::class.java)
        }
    }
}
