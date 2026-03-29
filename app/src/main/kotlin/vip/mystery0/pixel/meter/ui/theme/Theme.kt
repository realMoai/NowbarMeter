package com.kakao.taxi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun PixelPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isOledTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var colorScheme =
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    if (darkTheme && isOledTheme) {
        colorScheme = colorScheme.copy(
            surface = Color.Black,
            background = Color.Black,
            surfaceVariant = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
