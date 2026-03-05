package cat.company.wandervault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * Stateful entry point for the Calendar tab.
 *
 * Shares the [ItineraryViewModel] instance (same Koin scope as [ItineraryTabContent]) so no
 * extra data-fetching is needed.
 *
 * @param tripId The ID of the trip whose calendar is shown.
 * @param innerPadding Scaffold inner padding forwarded from the parent [Scaffold].
 */
@Composable
internal fun CalendarTabContent(
    tripId: Int,
    innerPadding: PaddingValues,
    viewModel: ItineraryViewModel = koinViewModel(parameters = { parametersOf(tripId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarContent(
        destinations = uiState.destinations,
        isLoading = uiState.isLoading,
        innerPadding = innerPadding,
    )
}

/**
 * Stateless calendar view for a trip.
 *
 * Displays a monthly grid where days belonging to a destination stay (between arrival and
 * departure) are highlighted with a band in [MaterialTheme.colorScheme.primaryContainer].
 * Arrival and departure days additionally get a [MaterialTheme.colorScheme.primary] circle.
 * Below the grid a legend lists each stay visible in the displayed month with the destination
 * name and its date range. The user can navigate between months using the previous/next arrows
 * in the header.
 *
 * @param destinations The list of destinations for the trip.
 * @param isLoading Whether the destinations are still being loaded.
 * @param innerPadding Scaffold inner padding to avoid content hidden behind bars.
 */
@Composable
internal fun CalendarContent(
    destinations: List<Destination>,
    isLoading: Boolean,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // The earliest date across all destinations, used to jump to the right month on first load.
    val eventMonth: YearMonth? = remember(destinations) {
        destinations
            .flatMap {
                listOfNotNull(
                    it.arrivalDateTime?.toLocalDate(),
                    it.departureDateTime?.toLocalDate(),
                )
            }
            .minOrNull()
            ?.let { YearMonth.from(it) }
    }

    // Persist the displayed month as an ISO "YYYY-MM" string so it survives config changes.
    // hasJumpedToEventMonth ensures we jump to the trip's first event month only once,
    // so manual navigation by the user is preserved across destination updates.
    var displayedMonthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var hasJumpedToEventMonth by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(eventMonth) {
        if (!hasJumpedToEventMonth && eventMonth != null) {
            hasJumpedToEventMonth = true
            displayedMonthStr = eventMonth.toString()
        }
    }
    val displayedMonth = YearMonth.parse(displayedMonthStr)

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Derive the locale's first day of the week so weekday columns and leading-blank offsets
    // are consistent across locales (e.g. Sunday-first in the US, Monday-first in Europe).
    // LocalConfiguration is used as the key so the value is recalculated on locale changes.
    val configuration = LocalConfiguration.current
    val weekStartDay: DayOfWeek = remember(configuration) {
        WeekFields.of(configuration.locales[0]).firstDayOfWeek
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        CalendarMonthHeader(
            yearMonth = displayedMonth,
            onPreviousMonth = { displayedMonthStr = displayedMonth.minusMonths(1).toString() },
            onNextMonth = { displayedMonthStr = displayedMonth.plusMonths(1).toString() },
        )
        CalendarWeekdayRow(weekStartDay = weekStartDay)
        CalendarMonthGrid(
            yearMonth = displayedMonth,
            destinations = destinations,
            weekStartDay = weekStartDay,
        )
        StayLegendSection(
            destinations = destinations,
            displayedMonth = displayedMonth,
        )
    }
}

/**
 * Header row showing the current month and year with previous/next navigation arrows.
 *
 * The month–year label is formatted with the locale-default pattern so that locales that order
 * "Year Month" (e.g. Japanese, Chinese) display correctly.
 */
@Composable
private fun CalendarMonthHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val monthYearFormatter = remember(configuration) {
        DateTimeFormatter.ofPattern("MMMM yyyy", configuration.locales[0])
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.calendar_previous_month),
            )
        }
        Text(
            text = yearMonth.atDay(1).format(monthYearFormatter),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.calendar_next_month),
            )
        }
    }
}

/**
 * A row of single-letter weekday abbreviations starting on the locale's first day of the week.
 *
 * @param weekStartDay The first day of the week for the current locale (e.g. Sunday in the US,
 *   Monday in most of Europe). Derived from [WeekFields.of].
 */
@Composable
private fun CalendarWeekdayRow(weekStartDay: DayOfWeek) {
    val locale = LocalConfiguration.current.locales[0]
    // Reorder weekdays so the column sequence begins on the locale's first day.
    val weekdays = remember(weekStartDay, locale) {
        DayOfWeek.entries.sortedBy { (it.value - weekStartDay.value + 7) % 7 }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

/** Holds the arrival and departure [LocalDate]s for a destination stay. */
private data class StayRange(val arrival: LocalDate, val departure: LocalDate)

/**
 * A 7-column grid showing every day in [yearMonth].
 *
 * Leading blank cells are added before the first day so that columns align with the correct
 * weekday. Days whose date falls within a destination stay (arrival through departure inclusive)
 * are rendered with a coloured band; arrival and departure days additionally show a highlighted
 * circle. The band runs edge-to-edge across cells so that it appears seamless across a full row.
 * When the stay wraps to a new row the band restarts at the left edge of that row.
 *
 * @param weekStartDay The locale's first day of the week; must match the column order in
 *   [CalendarWeekdayRow] so that days land in the correct columns.
 */
@Composable
private fun CalendarMonthGrid(
    yearMonth: YearMonth,
    destinations: List<Destination>,
    weekStartDay: DayOfWeek,
) {
    val firstOfMonth = yearMonth.atDay(1)
    // Number of blank cells before day 1 so it falls in the correct weekday column.
    val leadingBlanks = (firstOfMonth.dayOfWeek.value - weekStartDay.value + 7) % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    // Pre-compute event dates (any arrival or departure) and complete stay ranges.
    val eventDates: Set<LocalDate> = remember(destinations) {
        buildSet {
            destinations.forEach { dest ->
                dest.arrivalDateTime?.toLocalDate()?.let { add(it) }
                dest.departureDateTime?.toLocalDate()?.let { add(it) }
            }
        }
    }
    // A "complete stay" is a destination that has both arrival and departure dates.
    val stayRanges: List<StayRange> = remember(destinations) {
        destinations.mapNotNull { dest ->
            val arrival = dest.arrivalDateTime?.toLocalDate() ?: return@mapNotNull null
            val departure = dest.departureDateTime?.toLocalDate() ?: return@mapNotNull null
            StayRange(arrival, departure)
        }
    }

    val totalCells = leadingBlanks + daysInMonth
    val rowCount = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rowCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < leadingBlanks || cellIndex >= totalCells) {
                        // Blank spacer to keep days aligned with their weekday column.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    } else {
                        val dayIndex = cellIndex - leadingBlanks
                        val day = dayIndex + 1
                        val date = yearMonth.atDay(day)
                        val stayRange = stayRanges.firstOrNull { (arrival, departure) ->
                            !date.isBefore(arrival) && !date.isAfter(departure)
                        }
                        DayCell(
                            modifier = Modifier.weight(1f),
                            day = day,
                            isEventDay = date in eventDates,
                            isInStay = stayRange != null,
                            isStayStart = stayRange != null && stayRange.arrival == date,
                            isStayEnd = stayRange != null && stayRange.departure == date,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single day cell in the calendar grid.
 *
 * When [isInStay] is `true` a [MaterialTheme.colorScheme.primaryContainer] band is drawn
 * across the full cell width.  The band has rounded corners on the arrival side
 * ([isStayStart]) and on the departure side ([isStayEnd]); cells in the middle of a stay row
 * have straight edges so that adjacent bands appear seamless.
 *
 * When [isEventDay] is `true` (arrival or departure) a [MaterialTheme.colorScheme.primary]
 * circle is drawn on top of the band so the exact event day stands out at a glance.
 */
@Composable
private fun DayCell(
    day: Int,
    isEventDay: Boolean,
    isInStay: Boolean,
    isStayStart: Boolean,
    isStayEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    // The cell occupies its grid weight at a 1:1 aspect ratio with no horizontal padding so
    // that stay bands from adjacent cells touch each other edge-to-edge.
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // Stay band: a horizontally-spanning rectangle centred in the cell.
        if (isInStay) {
            val cornerRadius = 16.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(
                            topStart = if (isStayStart) cornerRadius else 0.dp,
                            bottomStart = if (isStayStart) cornerRadius else 0.dp,
                            topEnd = if (isStayEnd) cornerRadius else 0.dp,
                            bottomEnd = if (isStayEnd) cornerRadius else 0.dp,
                        ),
                    ),
            )
        }

        // Event circle (arrival or departure) rendered on top of the band.
        if (isEventDay) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        // Day number — colour adapts to the background so it remains legible.
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isEventDay -> MaterialTheme.colorScheme.onPrimary
                isInStay -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> Color.Unspecified
            },
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A section below the calendar grid listing all destination stays that overlap the displayed
 * month.  Each item shows a coloured dot, the destination name, and the full stay date range
 * so the user can identify which band in the grid belongs to which destination.
 *
 * The section is omitted entirely when no stays overlap the displayed month.
 */
@Composable
private fun StayLegendSection(
    destinations: List<Destination>,
    displayedMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    // Only show destinations that have a complete stay overlapping the displayed month.
    val staysInMonth = remember(destinations, displayedMonth) {
        val monthStart = displayedMonth.atDay(1)
        val monthEnd = displayedMonth.atEndOfMonth()
        destinations.filter { dest ->
            val arrival = dest.arrivalDateTime?.toLocalDate() ?: return@filter false
            val departure = dest.departureDateTime?.toLocalDate() ?: return@filter false
            !arrival.isAfter(monthEnd) && !departure.isBefore(monthStart)
        }
    }

    if (staysInMonth.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_stays_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        staysInMonth.forEach { dest ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Column {
                    Text(
                        text = dest.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val arrivalStr = dest.arrivalDateTime?.toLocalDate()?.format(dateFormatter)
                    val departureStr = dest.departureDateTime?.toLocalDate()?.format(dateFormatter)
                    if (arrivalStr != null || departureStr != null) {
                        Text(
                            text = "${arrivalStr ?: ""} – ${departureStr ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarContentPreview() {
    val destinations = listOf(
        Destination(
            id = 1,
            tripId = 1,
            name = "Paris",
            position = 0,
            arrivalDateTime = LocalDateTime.of(2024, 6, 1, 10, 0),
            departureDateTime = LocalDateTime.of(2024, 6, 5, 14, 0),
        ),
        Destination(
            id = 2,
            tripId = 1,
            name = "Lyon",
            position = 1,
            arrivalDateTime = LocalDateTime.of(2024, 6, 5, 18, 0),
            departureDateTime = LocalDateTime.of(2024, 6, 10, 9, 0),
        ),
    )
    WanderVaultTheme {
        CalendarContent(
            destinations = destinations,
            isLoading = false,
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarContentLoadingPreview() {
    WanderVaultTheme {
        CalendarContent(
            destinations = emptyList(),
            isLoading = true,
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarContentEmptyPreview() {
    WanderVaultTheme {
        CalendarContent(
            destinations = emptyList(),
            isLoading = false,
            innerPadding = PaddingValues(0.dp),
        )
    }
}
