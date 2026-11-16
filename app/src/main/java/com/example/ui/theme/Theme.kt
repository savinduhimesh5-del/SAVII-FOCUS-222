package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.domain.ThemeMode

private val SaviiDarkColorScheme = darkColorScheme(
    primary = VioletPrimaryDark,
    onPrimary = OnVioletDark,
    primaryContainer = VioletContainerDark,
    onPrimaryContainer = OnVioletContainerDark,
    secondary = LavenderSecondaryDark,
    onSecondary = OnLavenderDark,
    secondaryContainer = LavenderContainerDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = MintFocusDark,
    onTertiary = OnMintDark,
    tertiaryContainer = MintContainerDark,
    onTertiaryContainer = OnMintContainerDark,
    error = RoseErrorDark,
    onError = OnRoseErrorDark,
    errorContainer = RoseErrorContainerDark,
    onErrorContainer = OnRoseErrorContainerDark,
    background = MidnightObsidian,
    onBackground = TextPrimaryDark,
    surface = DeepVoid,
    onSurface = TextPrimaryDark,
    surfaceVariant = NightCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = MidnightObsidian,
    surfaceContainerLow = NightCardLow,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightCardHigh,
    surfaceContainerHighest = ColorTokens.NightCardHighest,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant
)

private val SaviiLightColorScheme = lightColorScheme(
    primary = VioletPrimaryLight,
    onPrimary = OnVioletLight,
    primaryContainer = VioletContainerLight,
    onPrimaryContainer = OnVioletContainerLight,
    secondary = LavenderSecondaryLight,
    onSecondary = OnLavenderLight,
    secondaryContainer = LavenderContainerLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = TealFocusLight,
    onTertiary = OnTealLight,
    tertiaryContainer = TealContainerLight,
    onTertiaryContainer = OnTealContainerLight,
    error = RoseErrorLight,
    onError = OnRoseErrorLight,
    errorContainer = RoseErrorContainerLight,
    onErrorContainer = OnRoseErrorContainerLight,
    background = CloudLavender,
    onBackground = TextPrimaryLight,
    surface = PureSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = PureSurfaceLight,
    surfaceContainerLow = LightCardLow,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCardHigh,
    surfaceContainerHighest = ColorTokens.LightCardHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private object ColorTokens {
    val NightCardHighest = androidx.compose.ui.graphics.Color(0xFF2E2957)
    val LightCardHighest = androidx.compose.ui.graphics.Color(0xFFD7CAF0)
}

val SaviiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SaviiFocusTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> SaviiDarkColorScheme
        else -> SaviiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SaviiTypography,
        shapes = SaviiShapes,
        content = content
    )
}
