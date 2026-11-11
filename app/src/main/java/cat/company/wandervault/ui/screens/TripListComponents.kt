package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/** Height of the cover area of a [TripCoverCard]. */
private val TripCoverHeight = 176.dp

/**
 * Photo-led card used to present a [Trip] in the Home, Favorites and Archive lists.
 *
 * The trip's cover image fills the card, with the title and dates overlaid on a gradient scrim.
 * Trips without a cover image get a branded gradient placeholder instead, so every card has the
 * same visual weight. Upcoming and ongoing trips show a small status badge in the top-start
 * corner.
 *
 * @param trip The trip to display.
 * @param onClick Called when the card is tapped.
 * @param modifier Modifier applied to the card.
 * @param coverModifier Modifier applied to the cover container when the trip has an image
 *   (e.g. a shared-element bounds modifier).
 * @param desaturated When `true` the cover image is rendered in greyscale (used for archived trips).
 * @param actions Action buttons shown in the top-end corner. They inherit a content colour that
 *   is legible on top of the cover.
 */
@Composable
internal fun TripCoverCard(
    trip: Trip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
    desaturated: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val hasImage = trip.imageUri != null
    val overlayContentColor = if (hasImage) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TripCoverHeight)
                .then(if (hasImage) coverModifier else Modifier),
        ) {
            if (hasImage) {
                val greyscale = remember(desaturated) {
                    if (desaturated) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else {
                        null
                    }
                }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(trip.imageUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.trip_image_content_desc),
                    contentScale = ContentScale.Crop,
                    colorFilter = greyscale,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(CoverScrim),
                )
            } else {
                TripCoverPlaceholder(modifier = Modifier.matchParentSize())
            }

            CompositionLocalProvider(LocalContentColor provides overlayContentColor) {
                TripStatusBadge(
                    trip = trip,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TripDateLine(trip = trip)
                }
            }
        }
    }
}

/** Top and bottom scrim that keeps overlaid text and icons legible on any photo. */
private val CoverScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.35f),
    0.3f to Color.Transparent,
    0.5f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.7f),
)

/** Branded gradient with a large, faded plane shown when a trip has no cover image. */
@Composable
private fun TripCoverPlaceholder(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)),
        ),
    ) {
        Icon(
            imageVector = Icons.Outlined.Flight,
            contentDescription = null,
            tint = colors.onPrimaryContainer.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 24.dp)
                .size(160.dp)
                .rotate(45f),
        )
    }
}

/** Date range and trip length, e.g. "Jun 1, 2024 – Jun 10, 2024 · 10 days". */
@Composable
private fun TripDateLine(trip: Trip) {
    val start = trip.startDate ?: return
    val end = trip.endDate ?: return
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val days = (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(
                R.string.trip_card_date_range,
                start.format(formatter),
                end.format(formatter),
                pluralStringResource(R.plurals.trip_card_duration_days, days, days),
            ),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Pill showing whether a trip is happening now or how soon it starts. Nothing is shown for past
 * trips or trips without dates.
 */
@Composable
private fun TripStatusBadge(trip: Trip, modifier: Modifier = Modifier) {
    val start = trip.startDate ?: return
    val end = trip.endDate ?: start
    val today = remember { LocalDate.now() }
    val (label, ongoing) = when {
        today.isAfter(end) -> return
        !today.isBefore(start) -> stringResource(R.string.trip_status_ongoing) to true
        else -> {
            val days = ChronoUnit.DAYS.between(today, start).toInt()
            pluralStringResource(R.plurals.trip_status_starts_in_days, days, days) to false
        }
    }
    Surface(
        shape = CircleShape,
        color = if (ongoing) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (ongoing) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Large title shown at the top of a top-level list screen.
 *
 * @param title The screen title.
 * @param subtitle Optional supporting line shown below the title.
 */
@Composable
internal fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier = modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Friendly empty state: an icon in a tonal circle followed by a title and explanation.
 *
 * @param icon Illustrative icon for the empty list.
 * @param title Short headline, e.g. "No trips yet".
 * @param message Supporting text explaining how to populate the list.
 */
@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripCoverCardPreview() {
    val today = LocalDate.now()
    WanderVaultTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TripCoverCard(
                trip = Trip(1, "Tokyo Adventure", startDate = today.plusDays(12), endDate = today.plusDays(21)),
                onClick = {},
            )
            TripCoverCard(trip = Trip(2, "Weekend in Lisbon"), onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    WanderVaultTheme {
        EmptyState(
            icon = Icons.Outlined.Flight,
            title = "No trips yet",
            message = "Tap \"New trip\" to start planning your next adventure",
        )
    }
}
