package cat.company.wandervault.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

/**
 * Document Info screen entry point.
 *
 * Displays a preview of the document file (image) or a generic icon (other types), the last
 * saved AI description, and basic file metadata (name, MIME type, size, folder). Also provides
 * an "Analyze Document" action that runs ML Kit document analysis inline.
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
        onOpenDocument = { document -> openTripDocument(context, document) },
        onAnalyzeDocument = viewModel::analyzeDocument,
        onAnalyzeApplyChanges = viewModel::applyAnalysisChanges,
        onAnalyzeFlightLegSelected = viewModel::onFlightLegSelected,
        onAnalyzeFlightConfirmed = viewModel::onFlightConfirmed,
        onAnalyzeHotelDestinationSelected = viewModel::onHotelDestinationSelected,
        onAnalyzeHotelConfirmed = viewModel::onHotelConfirmed,
        onAnalyzeTripInfoConfirmed = viewModel::onTripInfoConfirmed,
        onAnalyzeDismiss = viewModel::dismissAnalyze,
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
    onAnalyzeDocument: () -> Unit = {},
    onAnalyzeApplyChanges: () -> Unit = {},
    onAnalyzeFlightLegSelected: (TransportLeg) -> Unit = {},
    onAnalyzeFlightConfirmed: () -> Unit = {},
    onAnalyzeHotelDestinationSelected: (Destination) -> Unit = {},
    onAnalyzeHotelConfirmed: () -> Unit = {},
    onAnalyzeTripInfoConfirmed: () -> Unit = {},
    onAnalyzeDismiss: () -> Unit = {},
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
                        IconButton(onClick = onAnalyzeDocument) {
                            Icon(
                                imageVector = Icons.Default.FindInPage,
                                contentDescription = stringResource(R.string.documents_analyze_action),
                            )
                        }
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

    // Show the unified analysis dialog whenever an analysis is active. A single AlertDialog
    // composable is used for all states so that the same dialog window persists throughout the
    // entire analysis flow, avoiding spurious onDismissRequest calls on state transitions.
    val analyzeState = (uiState as? DocumentInfoUiState.Success)?.analyzeState
    if (analyzeState != null) {
        AnalyzeDocumentDialog(
            analyzeState = analyzeState,
            onApplyChanges = onAnalyzeApplyChanges,
            onFlightLegSelected = onAnalyzeFlightLegSelected,
            onFlightConfirmed = onAnalyzeFlightConfirmed,
            onHotelDestinationSelected = onAnalyzeHotelDestinationSelected,
            onHotelConfirmed = onAnalyzeHotelConfirmed,
            onTripInfoConfirmed = onAnalyzeTripInfoConfirmed,
            onDismiss = onAnalyzeDismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentInfoSuccessContent(
    uiState: DocumentInfoUiState.Success,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
        ),
    )
    val scope = rememberCoroutineScope()
    val isSheetHidden by remember {
        derivedStateOf {
            scaffoldState.bottomSheetState.currentValue == SheetValue.Hidden &&
                scaffoldState.bottomSheetState.targetValue == SheetValue.Hidden
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                DocumentInfoSheetContent(uiState = uiState)
            },
            sheetDragHandle = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    BottomSheetDefaults.DragHandle()
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { scope.launch { scaffoldState.bottomSheetState.hide() } },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.document_info_hide_sheet),
                        )
                    }
                }
            },
            sheetPeekHeight = DOCUMENT_INFO_SHEET_PEEK_HEIGHT,
            modifier = Modifier.fillMaxSize(),
        ) {
            ZoomableDocumentPreview(
                document = uiState.document,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isSheetHidden) {
            FilledTonalIconButton(
                onClick = { scope.launch { scaffoldState.bottomSheetState.partialExpand() } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.document_info_show_sheet),
                )
            }
        }
    }
}

@Composable
private fun DocumentInfoSheetContent(
    uiState: DocumentInfoUiState.Success,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ── File info ─────────────────────────────────────────────────────────

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
            value = uiState.folderName
                ?: if (uiState.document.folderId == null) {
                    stringResource(R.string.document_info_folder_root)
                } else {
                    stringResource(android.R.string.unknownName)
                },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── AI description ────────────────────────────────────────────────────

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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Wraps [DocumentPreview] with pinch-to-zoom (1×–[MAX_ZOOM]×) and pan support.
 *
 * Zoom and pan state are intentionally kept local to this composable: they are purely
 * ephemeral visual state that does not need to survive configuration changes or be shared
 * with the ViewModel. Double-tapping resets the view to the original scale and position.
 *
 * For multi-page PDFs, page-navigation state is also kept here so that the [PdfPageNavigator]
 * overlay sits outside the [graphicsLayer] transform, keeping it unaffected by zoom/pan.
 * Navigating to a new page resets zoom and pan to their initial values.
 */
@Composable
private fun ZoomableDocumentPreview(
    document: TripDocument,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var currentPage by remember(document.uri) { mutableIntStateOf(0) }
    var pageCount by remember(document.uri) { mutableIntStateOf(0) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset = if (scale <= MIN_ZOOM) Offset.Zero else offset + panChange
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = MIN_ZOOM
                    offset = Offset.Zero
                })
            }
            .transformable(state = transformState),
    ) {
        DocumentPreview(
            document = document,
            currentPage = currentPage,
            onPageCountAvailable = { pageCount = it },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )

        if (pageCount > 1) {
            PdfPageNavigator(
                currentPage = currentPage,
                pageCount = pageCount,
                onPreviousPage = {
                    currentPage--
                    scale = MIN_ZOOM
                    offset = Offset.Zero
                },
                onNextPage = {
                    currentPage++
                    scale = MIN_ZOOM
                    offset = Offset.Zero
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun DocumentPreview(
    document: TripDocument,
    currentPage: Int = 0,
    onPageCountAvailable: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isImage = document.mimeType.startsWith("image/")
    val isPdf = document.mimeType == "application/pdf"
    when {
        isImage && document.uri.isNotBlank() -> {
            val context = LocalContext.current
            val imageRequest = remember(document.uri) {
                ImageRequest.Builder(context)
                    .data(document.uri.toUri())
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.document_info_preview_content_desc, document.name),
                contentScale = ContentScale.Fit,
                modifier = modifier,
            )
        }
        isPdf && document.uri.isNotBlank() -> {
            val context = LocalContext.current
            // State: false = still loading; true = done (bitmap = success, null bitmap = failed/empty).
            // Keyed on both uri and currentPage so it re-renders whenever the page changes.
            val pdfRenderState by produceState<Pair<Boolean, Bitmap?>>(
                initialValue = false to null,
                key1 = document.uri,
                key2 = currentPage,
            ) {
                val result = withContext(Dispatchers.IO) {
                    renderPdfPage(context, document.uri, currentPage)
                }
                if (result != null) onPageCountAvailable(result.first)
                value = true to result?.second
            }
            val (done, bitmap) = pdfRenderState
            when {
                !done -> {
                    Box(
                        modifier = modifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                bitmap != null -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.document_info_preview_content_desc, document.name),
                        contentScale = ContentScale.Fit,
                        modifier = modifier,
                    )
                }
                else -> {
                    Box(
                        modifier = modifier,
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
        }
        else -> {
            Box(
                modifier = modifier,
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
}

/**
 * Renders page [pageIndex] of a PDF at [fileUri] to a [Bitmap] scaled to fit within
 * [maxDimension] pixels on the longest side, bounding memory use for large/scanned PDFs.
 * Also returns the total page count so callers can drive page-navigation UI.
 *
 * Supports both `file://` and `content://` URIs. Returns `null` if the file cannot be opened
 * or the PDF has no pages. Returns `Pair(pageCount, null)` if the file opened successfully
 * but the specific page could not be rendered.
 *
 * @throws kotlinx.coroutines.CancellationException if the calling coroutine was cancelled.
 */
private fun renderPdfPage(
    context: Context,
    fileUri: String,
    pageIndex: Int = 0,
    maxDimension: Int = 1080,
): Pair<Int, Bitmap?>? {
    val uri = Uri.parse(fileUri)
    val pfd = try {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("DocumentInfoScreen", "Failed to open PDF file $fileUri", e)
        return null
    }
    return pfd.use { descriptor ->
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("DocumentInfoScreen", "Failed to create PdfRenderer for $fileUri", e)
            return null
        }
        renderer.use { r ->
            val pageCount = r.pageCount
            if (pageCount == 0) return null
            val safeIndex = pageIndex.coerceIn(0, pageCount - 1)
            val bitmap = try {
                r.openPage(safeIndex).use { page ->
                    val scale = minOf(1f, maxDimension.toFloat() / maxOf(page.width, page.height))
                    // coerceAtLeast(1) guards against zero-dimension bitmaps due to float rounding.
                    val bitmapWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DocumentInfoScreen", "Failed to render PDF page $pageIndex for $fileUri", e)
                null
            }
            pageCount to bitmap
        }
    }
}

/**
 * Overlay bar with previous/next page buttons and a "Page X of Y" indicator.
 *
 * Rendered as a sibling of [DocumentPreview] inside [ZoomableDocumentPreview]'s [Box],
 * so it is not affected by the zoom/pan [graphicsLayer] transform applied to the preview.
 *
 * @param currentPage 0-based index of the currently displayed page.
 * @param pageCount   Total number of pages in the document.
 */
@Composable
private fun PdfPageNavigator(
    currentPage: Int,
    pageCount: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 0,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.document_info_pdf_prev_page),
                )
            }
            Text(
                text = stringResource(R.string.document_info_pdf_page_indicator, currentPage + 1, pageCount),
                style = MaterialTheme.typography.bodySmall,
            )
            IconButton(
                onClick = onNextPage,
                enabled = currentPage < pageCount - 1,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.document_info_pdf_next_page),
                )
            }
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

// ── Constants ─────────────────────────────────────────────────────────────────

/** How much of the bottom sheet is visible when in its collapsed (peeked) state. */
private val DOCUMENT_INFO_SHEET_PEEK_HEIGHT = 120.dp

/** Minimum zoom scale for the document preview (no zoom-out below original size). */
private const val MIN_ZOOM = 1f

/** Maximum zoom scale for the document preview. */
private const val MAX_ZOOM = 5f

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
