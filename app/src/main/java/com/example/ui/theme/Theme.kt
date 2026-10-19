package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PeacockGreenPrimaryDark,
    onPrimary = PeacockGreenOnPrimaryDark,
    primaryContainer = PeacockGreenContainerDark,
    onPrimaryContainer = PeacockGreenOnContainerDark,
    secondary = AmberGoldSecondaryDark,
    onSecondary = AmberGoldOnSecondaryDark,
    secondaryContainer = AmberGoldContainerDark,
    onSecondaryContainer = AmberGoldOnContainerDark,
    tertiary = RubyTertiaryDark,
    onTertiary = RubyOnTertiaryDark,
    tertiaryContainer = RubyContainerDark,
    onTertiaryContainer = RubyOnContainerDark,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PeacockGreenPrimary,
    onPrimary = PeacockGreenOnPrimary,
    primaryContainer = PeacockGreenContainer,
    onPrimaryContainer = PeacockGreenOnContainer,
    secondary = AmberGoldSecondary,
    onSecondary = AmberGoldOnSecondary,
    secondaryContainer = AmberGoldContainer,
    onSecondaryContainer = AmberGoldOnContainer,
    tertiary = RubyTertiary,
    onTertiary = RubyOnTertiary,
    tertiaryContainer = RubyContainer,
    onTertiaryContainer = RubyOnContainer,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant
)

@Composable
fun BurmeseTtsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted Burmese palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
