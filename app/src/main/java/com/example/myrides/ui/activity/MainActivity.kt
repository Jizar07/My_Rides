package com.example.myrides.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myrides.ui.screen.CombinedScreen
import com.example.myrides.ui.theme.MyRidesTheme
import com.example.myrides.service.LocationUpdatesService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
