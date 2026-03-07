package cat.company.wandervault.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Provides the [SharedTransitionScope] for shared-element transitions between the trip list and detail. */
@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Provides the [AnimatedVisibilityScope] for shared-element transitions between the trip list and detail. */
internal val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Applies a shared-bounds modifier for the trip cover image container when running inside a
 * [SharedTransitionLayout] / [AnimatedContent] scope, or returns [this] unchanged in other
 * contexts (e.g. `@Preview`).
 *
 * Use this on a [androidx.compose.foundation.layout.Box] that wraps the trip cover image (and
 * optionally overlaid UI controls), so that screens with differing container content share the
 * same animated bounds between the list card and the detail screen.
 *
 * @param tripId The unique ID of the trip, used to generate a stable shared-element key
 *   (`"trip-cover-image-$tripId"`) that matches between the list card and the detail screen.
 */
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun Modifier.sharedTripCoverBounds(tripId: Int): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current ?: return this
    return with(sharedTransitionScope) {
        this@sharedTripCoverBounds.sharedBounds(
            sharedContentState = rememberSharedContentState("trip-cover-image-$tripId"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

/**
 * Fades this element in and out in sync with the shared-element transition when running inside a
 * [SharedTransitionLayout] / [AnimatedContent] scope, or returns [this] unchanged in other
 * contexts (e.g. `@Preview`).
 *
 * Use this on UI chrome elements that should appear gradually alongside the shared-element
 * animation instead of popping in abruptly.
 */
@Composable
internal fun Modifier.fadeWithSharedTransition(): Modifier {
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current ?: return this
    return with(animatedVisibilityScope) {
        this@fadeWithSharedTransition.animateEnterExit(
            enter = fadeIn(),
            exit = fadeOut(),
        )
    }
}
