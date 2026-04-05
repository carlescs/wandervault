package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.company.wandervault.R

/**
 * A row showing the source document a piece of trip data was extracted from.
 *
 * Tapping the chip navigates to the source document; tapping the trailing ✕ icon removes
 * the link without affecting the data.
 *
 * @param documentName The display name of the source document, or `null` to show a generic label.
 * @param onDocumentClick Called when the user taps the chip to view the source document.
 * @param onRemove Called when the user taps the trailing close icon to remove the link.
 * @param modifier Optional [Modifier].
 */
@Composable
internal fun SourceDocumentChip(
    documentName: String?,
    onDocumentClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        AssistChip(
            onClick = onDocumentClick,
            label = {
                Text(
                    text = if (documentName != null) {
                        stringResource(R.string.source_document_label, documentName)
                    } else {
                        stringResource(R.string.source_document_fallback_label)
                    },
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.source_document_remove),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
