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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
 * Displays a monthly grid where days that have at least one destination arrival or departure are
 * highlighted with a primary-coloured background circle. The user can navigate between months
 * using the previous/next arrows in the header.
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
    // Collect all dates that have at least one destination arrival or departure.
    val eventDates: Set<LocalDate> = remember(destinations) {
        buildSet {
            for (dest in destinations) {
                dest.arrivalDateTime?.toLocalDate()?.let { add(it) }
                dest.departureDateTime?.toLocalDate()?.let { add(it) }
            }
        }
    }

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
            .padding(innerPadding),
    ) {
        CalendarMonthHeader(
            yearMonth = displayedMonth,
            onPreviousMonth = { displayedMonthStr = displayedMonth.minusMonths(1).toString() },
            onNextMonth = { displayedMonthStr = displayedMonth.plusMonths(1).toString() },
        )
        CalendarWeekdayRow(weekStartDay = weekStartDay)
        CalendarMonthGrid(
            yearMonth = displayedMonth,
            eventDates = eventDates,
            weekStartDay = weekStartDay,
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

/**
 * A 7-column grid showing every day in [yearMonth].
 *
 * Leading blank cells are added before the first day so that columns align with the correct
 * weekday. Days whose date is in [eventDates] are rendered with a highlighted [DayCell].
 *
 * @param weekStartDay The locale's first day of the week; must match the column order in
 *   [CalendarWeekdayRow] so that days land in the correct columns.
 */
@Composable
private fun CalendarMonthGrid(
    yearMonth: YearMonth,
    eventDates: Set<LocalDate>,
    weekStartDay: DayOfWeek,
) {
    val firstOfMonth = yearMonth.atDay(1)
    // Number of blank cells before day 1 so it falls in the correct weekday column.
    val leadingBlanks = (firstOfMonth.dayOfWeek.value - weekStartDay.value + 7) % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        // The whole month always fits in the available height; scrolling is not needed.
        // Month navigation is handled by the CalendarMonthHeader arrows instead.
        userScrollEnabled = false,
    ) {
        // Leading blank spacers to align the first day with its weekday column.
        items(leadingBlanks) {
            Box(modifier = Modifier.aspectRatio(1f))
        }
        // One cell per day of the month.
        items(daysInMonth) { dayIndex ->
            val day = dayIndex + 1
            val date = yearMonth.atDay(day)
            DayCell(day = day, hasEvent = date in eventDates)
        }
    }
}

/**
 * A single day cell in the calendar grid.
 *
 * When [hasEvent] is `true` the day number is displayed on a primary-coloured circle to make
 * event days easy to spot at a glance.
 */
@Composable
private fun DayCell(
    day: Int,
    hasEvent: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (hasEvent) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
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
