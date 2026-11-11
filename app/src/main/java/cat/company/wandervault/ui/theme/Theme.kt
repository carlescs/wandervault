package cat.company.wandervault.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * WanderVault default light color scheme.
 *
 * Uses a teal-primary palette inspired by ocean travel, paired with earthy
 * green and warm amber accents to reflect exploration and adventure. Every
 * Material 3 colour role is defined (including the surface-container roles) so
 * that no component falls back to the baseline purple palette.
 */
private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Neutral100,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    inversePrimary = Teal80,
    secondary = Green40,
    onSecondary = Neutral100,
    secondaryContainer = Green90,
    onSecondaryContainer = Green10,
    tertiary = Amber40,
    onTertiary = Neutral100,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Neutral100,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Teal40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    surfaceBright = Neutral98,
    surfaceDim = Neutral87,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

/**
 * WanderVault default dark color scheme.
 *
 * Inverts the brand palette so that teal, green, and amber surfaces remain
 * readable on dark backgrounds.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    inversePrimary = Teal40,
    secondary = Green80,
    onSecondary = Green20,
    secondaryContainer = Green30,
    onSecondaryContainer = Green90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Teal80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    surfaceBright = Neutral24,
    surfaceDim = Neutral6,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
)

/**
 * WanderVault application theme.
 *
 * Usage:
 * ```kotlin
 * WanderVaultTheme {
 *     // composables here automatically receive the brand color scheme,
 *     // typography, and shapes from MaterialTheme
 * }
 * ```
 *
 * Theme switching:
 * - Pass `darkTheme = true` to force dark mode regardless of system setting.
 * - Pass `dynamicColor = true` to use Android 12+ Material You wallpaper
 *   colors instead of the WanderVault brand palette.
 *
 * @param darkTheme  Whether to apply the dark color scheme. Defaults to the
 *                   system setting via [isSystemInDarkTheme].
 * @param dynamicColor  Whether to use dynamic (Material You) colors on
 *                      Android 12+. Defaults to `false` so the app keeps
 *                      its recognisable brand identity; when `true` the
 *                      brand palette is overridden by wallpaper colors.
 * @param content  Composable content displayed inside the theme.
 */
@Composable
fun WanderVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
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
        shapes = WanderVaultShapes,
        content = content
    )
}