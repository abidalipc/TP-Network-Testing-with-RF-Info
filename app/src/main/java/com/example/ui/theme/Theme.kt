package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val TelecomDarkColorScheme = darkColorScheme(
    primary = TelecomCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = TelecomCyanDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color.Black,
    background = TelecomNavyBg,
    onBackground = Color(0xFFF8FAFC),
    surface = TelecomSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = TelecomCardBorder,
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val TelecomLightColorScheme = lightColorScheme(
    primary = TelecomCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun SiteTracerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TelecomDarkColorScheme
        else -> TelecomLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
