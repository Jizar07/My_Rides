package com.example.myrides.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


/**
 * CombinedScreen handles the overall flow:
 * - It first displays a loading screen with a fading logo.
 * - After a 5‑second delay, it switches to the map screen.
 */
@Composable
fun CombinedScreen() {
    var showMap by remember { mutableStateOf(false) }

    // Wait for 5 seconds, then update the state to show the map.
    LaunchedEffect(Unit) {
        delay(5000)
        showMap = true
    }

    if (!showMap) {
        // Show the loading screen.
        LoadingScreen()
    } else {
        // Show the map screen.
        MapScreen()
    }
}
