package cat.company.wandervault.ui.screens

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The two anchor positions for the swipe-to-reveal gesture. */
private enum class RevealState { Closed, Revealed }

/** Width of the revealed action panel — matches the Material minimum touch-target size. */
private val REVEAL_WIDTH = 72.dp

/**
 * A two-step swipe-to-reveal container:
 * 1. Swipe toward the end edge to reveal the action icon.
 * 2. Tap the icon to confirm the action.
 *
 * The action is never triggered by the swipe alone; the user must explicitly tap the revealed icon.
 * Supports both LTR and RTL layouts.
 *
 * @param icon The icon shown in the revealed action panel.
 * @param iconContentDescription Accessibility description for the action icon.
 * @param containerColor Background colour of the action panel.
 * @param iconTint Tint applied to the action icon.
 * @param onAction Called when the user taps the revealed action icon.
 * @param modifier Modifier applied to the outer container.
 * @param content The foreground content (e.g. a trip card) that slides to reveal the panel.
 */
@Composable
internal fun SwipeToRevealBox(
    icon: ImageVector,
    iconContentDescription: String,
    containerColor: Color,
    iconTint: Color,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val revealWidthPx = with(density) { REVEAL_WIDTH.toPx() }
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // In LTR the foreground slides left (negative offset) to reveal the end panel;
    // in RTL it slides right (positive offset) to reveal the end panel.
    val revealAnchorPx = if (isRtl) revealWidthPx else -revealWidthPx

    val state = remember(revealWidthPx, isRtl) {
        AnchoredDraggableState(
            initialValue = RevealState.Closed,
            anchors = DraggableAnchors {
                RevealState.Closed at 0f
                RevealState.Revealed at revealAnchorPx
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Background action panel – its alpha tracks the drag progress for a smooth fade-in.
        // matchParentSize() ties this Box's size to the parent (i.e. the foreground content
        // height) instead of requesting unbounded constraints, which prevents it from
        // collapsing to zero height inside a LazyColumn.
        val progress by remember {
            derivedStateOf {
                val offset = state.offset
                if (offset.isNaN()) {
                    0f
                } else {
                    val raw = if (isRtl) offset / revealWidthPx else -offset / revealWidthPx
                    raw.coerceIn(0f, 1f)
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CardDefaults.shape)
                .background(containerColor.copy(alpha = progress))
                .padding(end = 12.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        state.animateTo(RevealState.Closed)
                        onAction()
                    }
                },
                enabled = state.currentValue == RevealState.Revealed,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    tint = iconTint.copy(alpha = progress),
                )
            }
        }

        // Foreground content – slides toward the end edge as the user drags,
        // revealing the action panel behind it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal),
        ) {
            content()
        }
    }
}

