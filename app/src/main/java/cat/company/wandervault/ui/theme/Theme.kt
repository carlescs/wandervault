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
 * green and warm amber accents to reflect exploration and adventure.
 */
private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Neutral99,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Green40,
    onSecondary = Neutral99,
    secondaryContainer = Green90,
    onSecondaryContainer = Green10,
    tertiary = Amber40,
    onTertiary = Neutral99,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant80,
    onSurfaceVariant = NeutralVariant30,
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
    primaryContainer = Teal40,
    onPrimaryContainer = Teal90,
    secondary = Green80,
    onSecondary = Green20,
    secondaryContainer = Green40,
    onSecondaryContainer = Green90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber40,
    onTertiaryContainer = Amber90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
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
 * - Pass `dynamicColor = false` to always use the WanderVault brand palette
 *   instead of Android 12+ Material You wallpaper colors.
 *
 * @param darkTheme  Whether to apply the dark color scheme. Defaults to the
 *                   system setting via [isSystemInDarkTheme].
 * @param dynamicColor  Whether to use dynamic (Material You) colors on
 *                      Android 12+. When `true` the brand palette is
 *                      overridden by the wallpaper-extracted colors.
 * @param content  Composable content displayed inside the theme.
 */
@Composable
fun WanderVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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