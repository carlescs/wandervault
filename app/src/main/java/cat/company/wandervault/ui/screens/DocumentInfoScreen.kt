package cat.company.wandervault.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

/**
 * Document Info screen entry point.
 *
 * Displays a preview of the document file (image) or a generic icon (other types), the last
 * saved AI description, and basic file metadata (name, MIME type, size, folder).
 *
 * @param documentId The ID of the document to display.
 * @param onNavigateUp Called when the user taps the back/up button.
 * @param modifier Optional [Modifier].
 */
@Composable
fun DocumentInfoScreen(
    documentId: Int,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentInfoViewModel = koinViewModel(
        key = "DocumentInfoViewModel:$documentId",
        parameters = { parametersOf(documentId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DocumentInfoContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onOpenDocument = { document ->
            try {
                val uri = document.uri.toUri()
                val contentUri = if (uri.scheme == "file") {
                    val path = uri.path
                        ?: throw IllegalArgumentException("File URI has no path: $uri")
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path),
                    )
                } else {
                    uri
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, document.mimeType.ifBlank { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.documents_no_app_to_open),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            } catch (_: IllegalArgumentException) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.documents_no_app_to_open),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        },
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Document Info screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentInfoContent(
    uiState: DocumentInfoUiState,
    onNavigateUp: () -> Unit,
    onOpenDocument: (TripDocument) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val title = if (uiState is DocumentInfoUiState.Success) {
        uiState.document.name
    } else {
        stringResource(R.string.document_info_title)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.document_info_navigate_up),
                        )
                    }
                },
                actions = {
                    if (uiState is DocumentInfoUiState.Success) {
                        IconButton(onClick = { onOpenDocument(uiState.document) }) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = stringResource(R.string.documents_open_action),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is DocumentInfoUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is DocumentInfoUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.document_info_not_found))
                }
            }

            is DocumentInfoUiState.Success -> {
                DocumentInfoSuccessContent(
                    uiState = uiState,
                    innerPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun DocumentInfoSuccessContent(
    uiState: DocumentInfoUiState.Success,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Document preview ──────────────────────────────────────────────────

        DocumentPreview(
            document = uiState.document,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── File info ─────────────────────────────────────────────────────────

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.document_info_section_info),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(
                label = stringResource(R.string.document_info_name),
                value = uiState.document.name,
            )
            InfoRow(
                label = stringResource(R.string.document_info_type),
                value = uiState.document.mimeType.ifBlank { stringResource(R.string.document_info_type_unknown) },
            )
            if (uiState.fileSizeBytes != null) {
                InfoRow(
                    label = stringResource(R.string.document_info_size),
                    value = formatFileSize(uiState.fileSizeBytes),
                )
            }
            InfoRow(
                label = stringResource(R.string.document_info_folder),
                value = uiState.folderName ?: stringResource(R.string.document_info_folder_root),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── AI description ────────────────────────────────────────────────────

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.document_info_section_ai_description),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val summary = uiState.document.summary
            if (summary.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.document_info_no_ai_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            } else {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DocumentPreview(
    document: TripDocument,
    modifier: Modifier = Modifier,
) {
    val isImage = document.mimeType.startsWith("image/")
    if (isImage && document.uri.isNotBlank()) {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(document.uri.toUri())
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.document_info_preview_content_desc, document.name),
            contentScale = ContentScale.Fit,
            modifier = modifier
                .height(240.dp)
                .fillMaxWidth(),
        )
    } else {
        Box(
            modifier = modifier
                .height(160.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = 1_024L
    val mb = kb * 1_024L
    return when {
        bytes < kb -> "$bytes B"
        bytes < mb -> "${bytes / kb} KB"
        else -> "%.1f MB".format(bytes.toDouble() / mb)
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun DocumentInfoLoadingPreview() {
    WanderVaultTheme {
        DocumentInfoContent(
            uiState = DocumentInfoUiState.Loading,
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentInfoNotFoundPreview() {
    WanderVaultTheme {
        DocumentInfoContent(
            uiState = DocumentInfoUiState.NotFound,
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentInfoNoSummaryPreview() {
    val document = TripDocument(
        id = 1,
        tripId = 1,
        name = "boarding_pass.pdf",
        uri = "",
        mimeType = "application/pdf",
        summary = null,
    )
    WanderVaultTheme {
        DocumentInfoContent(
            uiState = DocumentInfoUiState.Success(
                document = document,
                fileSizeBytes = 245_760L,
                folderName = "Flight Documents",
            ),
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentInfoWithSummaryPreview() {
    val document = TripDocument(
        id = 2,
        tripId = 1,
        name = "travel_insurance.pdf",
        uri = "",
        mimeType = "application/pdf",
        summary = "Travel insurance policy covering medical expenses up to €500,000, trip cancellation, and lost baggage. Policy number: TI-2024-98765. Valid from 2024-06-01 to 2024-06-10.",
    )
    WanderVaultTheme {
        DocumentInfoContent(
            uiState = DocumentInfoUiState.Success(
                document = document,
                fileSizeBytes = 1_234_567L,
                folderName = null,
            ),
            onNavigateUp = {},
        )
    }
}
