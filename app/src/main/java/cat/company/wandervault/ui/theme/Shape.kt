package cat.company.wandervault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * WanderVault shape scale.
 *
 * Slightly rounder than the Material 3 baseline so cards, sheets and dialogs feel soft and
 * friendly, matching the photo-led trip cards.
 */
val WanderVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
