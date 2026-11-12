package cat.company.wandervault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme

/**
 * Leading icon for an [AlertDialog]'s `icon` slot. Destructive dialogs tint the icon with the
 * error colour so the consequence is clear before the user reads the text.
 */
@Composable
internal fun DialogIcon(icon: ImageVector, isDestructive: Boolean = false) {
    if (isDestructive) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.error)
    } else {
        Icon(imageVector = icon, contentDescription = null)
    }
}

/** Text button for the confirming action of a destructive dialog, rendered in the error colour. */
@Composable
internal fun DestructiveTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Text(text)
    }
}

/**
 * Standard confirmation dialog for destructive actions (deleting a trip, destination, document,
 * etc.): error-tinted icon, centred title, explanation and an error-coloured confirm button.
 */
@Composable
internal fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Outlined.DeleteOutline,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { DialogIcon(icon, isDestructive = true) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { DestructiveTextButton(text = confirmLabel, onClick = onConfirm) },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** A spinner followed by a status [message], used for in-progress states inside dialogs. */
@Composable
internal fun DialogProgressRow(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Highlighted card shown at the top of import / review dialogs summarising the item found in a
 * document (flight, hotel, activity…).
 *
 * @param icon Icon representing the kind of item.
 * @param title Headline (e.g. "Vueling VY1234").
 * @param details Secondary lines shown under the headline; blank entries are skipped.
 */
@Composable
internal fun DialogSummaryCard(
    icon: ImageVector,
    title: String,
    details: List<String?> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(
                icon = icon,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                size = 40.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                details.filterNot { it.isNullOrBlank() }.forEach { line ->
                    Text(
                        text = line.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/**
 * A rounded row inside a dialog: icon badge, headline and optional supporting lines.
 *
 * When [onClick] is non-null the row is tappable and shows a trailing chevron (used for
 * pick-one-of-many selection dialogs). When [onClick] is `null` the row is a static,
 * emphasised preview of the item that will be affected (used by confirmation dialogs).
 *
 * @param details Secondary lines shown under [title]; blank entries are skipped.
 * @param titleMaxLines Maximum number of lines for [title] before it is ellipsised.
 */
@Composable
internal fun DialogOptionItem(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    details: List<String?> = emptyList(),
    titleMaxLines: Int = 2,
) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(icon = icon, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                details.filterNot { it.isNullOrBlank() }.forEach { line ->
                    Text(
                        text = line.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A labelled, full-width selector used inside dialogs: a caption above an outlined box that
 * shows the current [value] and a trailing drop-down arrow. Tapping it calls [onClick], which
 * typically opens a menu or a picker dialog.
 *
 * @param isPlaceholder Renders [value] in a muted colour when nothing has been chosen yet.
 */
@Composable
internal fun SelectorField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPlaceholder: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .padding(start = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaceholder) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Secondary explanatory text used between blocks inside dialogs. */
@Composable
internal fun DialogSupportingText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun DialogComponentsPreview() {
    WanderVaultTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogSummaryCard(
                icon = Icons.Outlined.Place,
                title = "Hotel Arts Barcelona",
                details = listOf("Ref: X7K2PQ"),
            )
            DialogSupportingText("Which destination should this hotel be added to?")
            DialogOptionItem(
                icon = Icons.Outlined.Place,
                title = "Barcelona",
                details = listOf("Jun 1, 2024 – Jun 3, 2024"),
                onClick = {},
            )
            DialogProgressRow(message = "Analysing document…")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DestructiveConfirmationDialogPreview() {
    WanderVaultTheme {
        DestructiveConfirmationDialog(
            title = "Remove stop?",
            message = "\"Paris\" will be permanently removed from the itinerary.",
            confirmLabel = "Remove",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
