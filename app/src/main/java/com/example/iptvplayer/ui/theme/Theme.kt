package com.example.iptvplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppColorScheme = darkColorScheme(
    primary = Color(0xFF8C7BFF),
    onPrimary = Color(0xFF1B0F5C),
    primaryContainer = Color(0xFF3D2E8C),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF2FDDC6),
    onSecondary = Color(0xFF00332E),
    background = Color(0xFF0D0D12),
    onBackground = Color(0xFFEBEBF2),
    surface = Color(0xFF17161F),
    onSurface = Color(0xFFEBEBF2),
    surfaceVariant = Color(0xFF262530),
    onSurfaceVariant = Color(0xFFC9C7D6),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0000),
    outline = Color(0xFF3C3A48)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun IPTVPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        shapes = AppShapes,
        content = content
    )
}
