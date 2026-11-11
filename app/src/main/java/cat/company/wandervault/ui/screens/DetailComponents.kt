package cat.company.wandervault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.company.wandervault.ui.theme.WanderVaultTheme

/**
 * Card used to group related content on detail screens.
 *
 * Renders a header row (leading [icon], [title] and optional trailing [actions]) above
 * [content]. Uses the low surface-container colour so sections read as one consistent family
 * across the Trip, Location and Transport detail screens.
 *
 * @param title Section heading.
 * @param icon Leading icon that hints at the section's meaning.
 * @param actions Optional trailing actions (e.g. refresh / delete icon buttons).
 * @param content Section body, laid out in a [Column].
 */
@Composable
internal fun DetailSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                actions()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                content = content,
            )
        }
    }
}

/** Above this many tabs, [DetailTabRow] scrolls horizontally instead of splitting the width. */
private const val MAX_FIXED_TABS = 3

/**
 * Tab row shown under the top app bar of a detail screen to switch between its sub-views.
 *
 * Uses a fixed [PrimaryTabRow] for up to [MAX_FIXED_TABS] tabs and a [PrimaryScrollableTabRow]
 * beyond that, so labels never get truncated on narrow phones.
 *
 * @param tabs The tabs to show, in order.
 * @param selectedTab The currently selected tab; must be one of [tabs].
 * @param onTabSelected Called when the user taps a tab.
 * @param labelRes Maps a tab to its label string resource.
 * @param icon Maps a tab to its leading icon.
 * @param containerColor Background of the row (e.g. transparent over a hero image).
 * @param selectedContentColor Colour of the selected tab's label and icon.
 * @param unselectedContentColor Colour of the other tabs' labels and icons.
 * @param showDivider Whether to draw the divider below the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> DetailTabRow(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    labelRes: (T) -> Int,
    icon: (T) -> ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showDivider: Boolean = true,
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val tabContent: @Composable () -> Unit = {
        tabs.forEach { tab ->
            LeadingIconTab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(stringResource(labelRes(tab)), maxLines = 1) },
                icon = { Icon(icon(tab), contentDescription = null) },
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
            )
        }
    }
    val divider: @Composable () -> Unit = { if (showDivider) HorizontalDivider() }
    if (tabs.size <= MAX_FIXED_TABS) {
        PrimaryTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = selectedContentColor,
            divider = divider,
            tabs = tabContent,
        )
    } else {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = selectedContentColor,
            edgePadding = 8.dp,
            divider = divider,
            tabs = tabContent,
        )
    }
}

/**
 * An icon centred in a filled circle, used as a leading visual for list rows on detail screens.
 */
@Composable
internal fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size / 2),
        )
    }
}

/**
 * Full-screen message used for error / not-found states on detail screens: an icon in a tonal
 * circle above a short explanation.
 */
@Composable
internal fun DetailMessageState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            icon = icon,
            size = 88.dp,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailSectionPreview() {
    WanderVaultTheme {
        DetailSection(
            title = "What's next",
            icon = Icons.Outlined.AutoAwesome,
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Check in to your hotel in Montmartre after 3 pm.")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailMessageStatePreview() {
    WanderVaultTheme {
        DetailMessageState(message = "Trip not found")
    }
}
