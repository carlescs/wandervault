package cat.company.wandervault.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Provides the [SharedTransitionScope] for shared-element transitions between the trip list and detail. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Provides the [AnimatedVisibilityScope] for shared-element transitions between the trip list and detail. */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Applies a shared-element modifier for the trip cover image when running inside a
 * [SharedTransitionLayout] / [AnimatedContent] scope, or returns [this] unchanged in other
 * contexts (e.g. `@Preview`).
 *
 * @param tripId The unique ID of the trip, used to generate a stable shared-element key
 *   (`"trip-cover-image-$tripId"`) that matches between the list card and the detail screen.
 */
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun Modifier.sharedTripCoverImage(tripId: Int): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current ?: return this
    return with(sharedTransitionScope) {
        this@sharedTripCoverImage.sharedElement(
            state = rememberSharedContentState("trip-cover-image-$tripId"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}
