package com.example.myrides.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.example.myrides.R
import androidx.compose.runtime.getValue


/**
 * LoadingScreen displays a logo that fades in.
 */
@Composable
fun LoadingScreen() {
    // Animate the logo's alpha value over 3 seconds.
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 3000)
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Replace R.drawable.newlogo with your actual logo resource.
        Image(
            painter = painterResource(id = R.drawable.newlogo_foreground),
            contentDescription = "Logo",
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )
    }
}
