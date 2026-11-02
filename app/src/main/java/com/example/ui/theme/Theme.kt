package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AguiaFlyRoyalSapphireColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = Color(0xFF07152B),
    primaryContainer = VelvetAzureElevated,
    onPrimaryContainer = BrushedGoldLight,
    secondary = MetallicAeroBlue,
    onSecondary = Color(0xFF07152B),
    secondaryContainer = ImperialCobaltSurface,
    onSecondaryContainer = PlatinumText,
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF041A12),
    background = RoyalSapphireBg,
    onBackground = PlatinumText,
    surface = ImperialCobaltSurface,
    onSurface = PlatinumText,
    surfaceVariant = VelvetAzureElevated,
    onSurfaceVariant = MutedSilver,
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AguiaFlyRoyalSapphireColorScheme,
        typography = Typography,
        content = content
    )
}
