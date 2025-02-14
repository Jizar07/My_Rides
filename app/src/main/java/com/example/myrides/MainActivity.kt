package com.example.myrides

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myrides.ui.CombinedScreen
import com.example.myrides.ui.theme.MyRidesTheme
import com.example.myrides.LocationUpdatesService  // Ensure this import matches your package structure

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the background location updates service.
        val locationServiceIntent = Intent(this, LocationUpdatesService::class.java)
        startService(locationServiceIntent)

        setContent {
            MyRidesTheme {
                CombinedScreen()
            }
        }
    }
}
