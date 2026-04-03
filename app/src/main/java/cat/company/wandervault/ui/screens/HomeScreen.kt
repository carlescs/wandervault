package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.ImageSearchResult
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.sharedTripCoverBounds
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Home screen – displays the user's list of trips.
 *
 * When no trips exist an empty-state message is shown.
 * A FAB opens the [AddTripDialog] to create a new trip.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = koinViewModel(), onTripClick: (Int) -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        onAddTripClick = viewModel::onAddTripClick,
        onDismissDialog = viewModel::onDismissAddTripDialog,
        onTitleChange = viewModel::onAddTripTitleChange,
        onImageUriChange = viewModel::onAddTripImageUriChange,
        onTimezoneChange = viewModel::onAddTripTimezoneChange,
        onSaveTrip = viewModel::onSaveTrip,
        onEditTripClick = viewModel::onEditTripClick,
        onDismissEditDialog = viewModel::onDismissEditTripDialog,
        onEditTitleChange = viewModel::onEditTripTitleChange,
        onEditImageUriChange = viewModel::onEditTripImageUriChange,
        onEditTimezoneChange = viewModel::onEditTripTimezoneChange,
        onUpdateTrip = viewModel::onUpdateTrip,
        onDeleteTripClick = viewModel::onDeleteTripClick,
        onConfirmDeleteTrip = viewModel::onConfirmDeleteTrip,
        onDismissDeleteDialog = viewModel::onDismissDeleteTripDialog,
        onFavoriteClick = viewModel::onToggleFavorite,
        onArchiveClick = viewModel::onArchiveTrip,
        onTripClick = onTripClick,
        onOpenImageSearchForAdd = { viewModel.onOpenImageSearch(forAdd = true) },
        onOpenImageSearchForEdit = { viewModel.onOpenImageSearch(forAdd = false) },
        onDismissImageSearch = viewModel::onDismissImageSearch,
        onImageSearchQueryChange = viewModel::onImageSearchQueryChange,
        onSearchImages = viewModel::onSearchImages,
        onSelectSearchImage = viewModel::onSelectSearchImage,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the home screen.
 *
 * Accepts a [HomeUiState] snapshot and event callbacks so it can be reused
 * in `@Preview` functions without a real [HomeViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenContent(
    uiState: HomeUiState,
    onAddTripClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onTitleChange: (String) -> Unit,
    onImageUriChange: (String?) -> Unit,
    onTimezoneChange: (String?) -> Unit = {},
    onSaveTrip: () -> Unit,
    onEditTripClick: (Trip) -> Unit,
    onDismissEditDialog: () -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditImageUriChange: (String?) -> Unit,
    onEditTimezoneChange: (String?) -> Unit = {},
    onUpdateTrip: () -> Unit,
    onDeleteTripClick: (Trip) -> Unit = {},
    onConfirmDeleteTrip: () -> Unit = {},
    onDismissDeleteDialog: () -> Unit = {},
    onFavoriteClick: (Trip) -> Unit = {},
    onArchiveClick: (Trip) -> Unit = {},
    onTripClick: (Int) -> Unit = {},
    onOpenImageSearchForAdd: () -> Unit = {},
    onOpenImageSearchForEdit: () -> Unit = {},
    onDismissImageSearch: () -> Unit = {},
    onImageSearchQueryChange: (String) -> Unit = {},
    onSearchImages: () -> Unit = {},
    onSelectSearchImage: (ImageSearchResult, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.trips.isEmpty()) {
            TripsEmptyState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.trips, key = { it.id }) { trip ->
                    val swipeState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(swipeState.currentValue) {
                        if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            onArchiveClick(trip)
                        }
                    }
                    SwipeToDismissBox(
                        state = swipeState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { SwipeToArchiveBackground(swipeState) },
                        modifier = Modifier.animateItem(),
                    ) {
                        TripCard(
                            trip = trip,
                            onEditClick = { onEditTripClick(trip) },
                            onDeleteClick = { onDeleteTripClick(trip) },
                            onFavoriteClick = { onFavoriteClick(trip) },
                            onArchiveClick = { onArchiveClick(trip) },
                            onCardClick = { onTripClick(trip.id) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddTripClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_trip_content_desc))
        }
    }

    if (uiState.showAddTripDialog) {
        AddTripDialog(
            title = uiState.addTripTitle,
            onTitleChange = onTitleChange,
            imageUri = uiState.addTripImageUri,
            onImageUriChange = onImageUriChange,
            timezone = uiState.addTripTimezone,
            onTimezoneChange = onTimezoneChange,
            isFormValid = uiState.isAddTripFormValid,
            onSave = onSaveTrip,
            onDismiss = onDismissDialog,
            onSearchOnline = onOpenImageSearchForAdd,
            showImageSearch = uiState.showImageSearchDialog,
            imageSearchQuery = uiState.imageSearchQuery,
            onImageSearchQueryChange = onImageSearchQueryChange,
            onSearchImages = onSearchImages,
            imageSearchLoading = uiState.imageSearchLoading,
            imageDownloading = uiState.imageDownloading,
            imageSearchResults = uiState.imageSearchResults,
            imageSearchError = uiState.imageSearchError,
            imageSearchNoResults = uiState.imageSearchNoResults,
            imageDownloadError = uiState.imageDownloadError,
            onSelectSearchImage = { result -> onSelectSearchImage(result, true) },
            onDismissImageSearch = onDismissImageSearch,
        )
    }

    if (uiState.showEditTripDialog) {
        EditTripDialog(
            title = uiState.editTripTitle,
            onTitleChange = onEditTitleChange,
            imageUri = uiState.editTripImageUri,
            onImageUriChange = onEditImageUriChange,
            timezone = uiState.editTripTimezone,
            onTimezoneChange = onEditTimezoneChange,
            isFormValid = uiState.isEditTripFormValid,
            onSave = onUpdateTrip,
            onDismiss = onDismissEditDialog,
            onSearchOnline = onOpenImageSearchForEdit,
            showImageSearch = uiState.showImageSearchDialog,
            imageSearchQuery = uiState.imageSearchQuery,
            onImageSearchQueryChange = onImageSearchQueryChange,
            onSearchImages = onSearchImages,
            imageSearchLoading = uiState.imageSearchLoading,
            imageDownloading = uiState.imageDownloading,
            imageSearchResults = uiState.imageSearchResults,
            imageSearchError = uiState.imageSearchError,
            imageSearchNoResults = uiState.imageSearchNoResults,
            imageDownloadError = uiState.imageDownloadError,
            onSelectSearchImage = { result -> onSelectSearchImage(result, false) },
            onDismissImageSearch = onDismissImageSearch,
        )
    }

    uiState.tripToDelete?.let { trip ->
        DeleteTripConfirmationDialog(
            tripTitle = trip.title,
            onConfirm = onConfirmDeleteTrip,
            onDismiss = onDismissDeleteDialog,
        )
    }
}

@Composable
private fun TripCard(trip: Trip, onEditClick: () -> Unit, onDeleteClick: () -> Unit = {}, onFavoriteClick: () -> Unit = {}, onArchiveClick: () -> Unit = {}, onCardClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Card(modifier = modifier.fillMaxWidth(), onClick = onCardClick) {
        if (trip.imageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .sharedTripCoverBounds(trip.id),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(trip.imageUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.trip_image_content_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (trip.startDate != null && trip.endDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${trip.startDate.format(formatter)} – ${trip.endDate.format(formatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconToggleButton(
                checked = trip.isFavorite,
                onCheckedChange = { onFavoriteClick() },
            ) {
                Icon(
                    imageVector = if (trip.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(if (trip.isFavorite) R.string.trip_remove_favorite else R.string.trip_add_favorite),
                    tint = if (trip.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_trip_content_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onArchiveClick) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = stringResource(R.string.archive_trip_content_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_trip_content_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToArchiveBackground(swipeState: SwipeToDismissBoxState) {
    val isActive = swipeState.targetValue == SwipeToDismissBoxValue.EndToStart
    val isSwiping = swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val containerColor by animateColorAsState(
        when {
            isActive -> MaterialTheme.colorScheme.secondaryContainer
            isSwiping -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = SWIPE_HINT_BG_ALPHA)
            else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f)
        },
        label = "archive_swipe_bg",
    )
    val iconTint by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.secondary.copy(alpha = SWIPE_HINT_ICON_ALPHA),
        label = "archive_icon_tint",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardDefaults.shape)
            .background(containerColor)
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AnimatedVisibility(
            visible = isSwiping || isActive,
            enter = fadeIn() + scaleIn(initialScale = 0.75f),
            exit = fadeOut() + scaleOut(targetScale = 0.75f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = iconTint,
                )
                AnimatedVisibility(visible = isActive) {
                    Text(
                        text = stringResource(R.string.archive_trip_content_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Shared dialog for creating or editing a trip.
 *
 * Used by [AddTripDialog] and [EditTripDialog] to avoid duplication.
 *
 * @param dialogTitle The title shown at the top of the dialog.
 * @param title The current trip name input value.
 * @param onTitleChange Called when the user changes the trip name.
 * @param imageUri The currently selected background image URI, or null if none chosen.
 * @param onImageUriChange Called when the user picks or removes a background image.
 * @param timezone The IANA timezone ID for this trip, or null for device default.
 * @param onTimezoneChange Called when the user selects a timezone.
 * @param isFormValid Whether the form inputs are valid, enabling the save button.
 * @param onSave Called when the user confirms the dialog.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 * @param onSearchOnline Called when the user wants to search for an image online.
 * @param showImageSearch Whether to show the inline image search dialog.
 * @param imageSearchQuery The current search query text.
 * @param onImageSearchQueryChange Called when the search query changes.
 * @param onSearchImages Called to trigger an image search.
 * @param imageSearchLoading True while a search is in progress.
 * @param imageDownloading True while a selected image is being downloaded.
 * @param imageSearchResults The current list of search results.
 * @param imageSearchError True when the search failed due to a network/API error.
 * @param imageSearchNoResults True when the search succeeded but returned no results.
 * @param imageDownloadError True when the download of a selected image failed.
 * @param onSelectSearchImage Called with the chosen [ImageSearchResult].
 * @param onDismissImageSearch Called when the image search dialog is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripFormDialog(
    dialogTitle: String,
    title: String,
    onTitleChange: (String) -> Unit,
    imageUri: String?,
    onImageUriChange: (String?) -> Unit,
    timezone: String?,
    onTimezoneChange: (String?) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onSearchOnline: () -> Unit = {},
    showImageSearch: Boolean = false,
    imageSearchQuery: String = "",
    onImageSearchQueryChange: (String) -> Unit = {},
    onSearchImages: () -> Unit = {},
    imageSearchLoading: Boolean = false,
    imageDownloading: Boolean = false,
    imageSearchResults: List<ImageSearchResult> = emptyList(),
    imageSearchError: Boolean = false,
    imageSearchNoResults: Boolean = false,
    imageDownloadError: Boolean = false,
    onSelectSearchImage: (ImageSearchResult) -> Unit = {},
    onDismissImageSearch: () -> Unit = {},
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        onImageUriChange(uri?.toString())
    }

    if (showImageSearch) {
        ImageSearchDialog(
            query = imageSearchQuery,
            onQueryChange = onImageSearchQueryChange,
            onSearch = onSearchImages,
            isLoading = imageSearchLoading,
            isDownloading = imageDownloading,
            results = imageSearchResults,
            hasError = imageSearchError,
            hasNoResults = imageSearchNoResults,
            hasDownloadError = imageDownloadError,
            onSelectImage = onSelectSearchImage,
            onDismiss = onDismissImageSearch,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.add_trip_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TimezoneDropdown(
                    selectedTimezone = timezone,
                    onTimezoneChange = onTimezoneChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(imageUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.trip_image_content_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { onImageUriChange(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.trip_image_remove))
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.trip_image_pick))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onSearchOnline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.trip_image_search_online))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = isFormValid) { Text(stringResource(R.string.add_trip_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun AddTripDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    imageUri: String?,
    onImageUriChange: (String?) -> Unit,
    timezone: String?,
    onTimezoneChange: (String?) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onSearchOnline: () -> Unit = {},
    showImageSearch: Boolean = false,
    imageSearchQuery: String = "",
    onImageSearchQueryChange: (String) -> Unit = {},
    onSearchImages: () -> Unit = {},
    imageSearchLoading: Boolean = false,
    imageDownloading: Boolean = false,
    imageSearchResults: List<ImageSearchResult> = emptyList(),
    imageSearchError: Boolean = false,
    imageSearchNoResults: Boolean = false,
    imageDownloadError: Boolean = false,
    onSelectSearchImage: (ImageSearchResult) -> Unit = {},
    onDismissImageSearch: () -> Unit = {},
) {
    TripFormDialog(
        dialogTitle = stringResource(R.string.add_trip_title),
        title = title,
        onTitleChange = onTitleChange,
        imageUri = imageUri,
        onImageUriChange = onImageUriChange,
        timezone = timezone,
        onTimezoneChange = onTimezoneChange,
        isFormValid = isFormValid,
        onSave = onSave,
        onDismiss = onDismiss,
        onSearchOnline = onSearchOnline,
        showImageSearch = showImageSearch,
        imageSearchQuery = imageSearchQuery,
        onImageSearchQueryChange = onImageSearchQueryChange,
        onSearchImages = onSearchImages,
        imageSearchLoading = imageSearchLoading,
        imageDownloading = imageDownloading,
        imageSearchResults = imageSearchResults,
        imageSearchError = imageSearchError,
        imageSearchNoResults = imageSearchNoResults,
        imageDownloadError = imageDownloadError,
        onSelectSearchImage = onSelectSearchImage,
        onDismissImageSearch = onDismissImageSearch,
    )
}

@Composable
private fun EditTripDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    imageUri: String?,
    onImageUriChange: (String?) -> Unit,
    timezone: String?,
    onTimezoneChange: (String?) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onSearchOnline: () -> Unit = {},
    showImageSearch: Boolean = false,
    imageSearchQuery: String = "",
    onImageSearchQueryChange: (String) -> Unit = {},
    onSearchImages: () -> Unit = {},
    imageSearchLoading: Boolean = false,
    imageDownloading: Boolean = false,
    imageSearchResults: List<ImageSearchResult> = emptyList(),
    imageSearchError: Boolean = false,
    imageSearchNoResults: Boolean = false,
    imageDownloadError: Boolean = false,
    onSelectSearchImage: (ImageSearchResult) -> Unit = {},
    onDismissImageSearch: () -> Unit = {},
) {
    TripFormDialog(
        dialogTitle = stringResource(R.string.edit_trip_title),
        title = title,
        onTitleChange = onTitleChange,
        imageUri = imageUri,
        onImageUriChange = onImageUriChange,
        timezone = timezone,
        onTimezoneChange = onTimezoneChange,
        isFormValid = isFormValid,
        onSave = onSave,
        onDismiss = onDismiss,
        onSearchOnline = onSearchOnline,
        showImageSearch = showImageSearch,
        imageSearchQuery = imageSearchQuery,
        onImageSearchQueryChange = onImageSearchQueryChange,
        onSearchImages = onSearchImages,
        imageSearchLoading = imageSearchLoading,
        imageDownloading = imageDownloading,
        imageSearchResults = imageSearchResults,
        imageSearchError = imageSearchError,
        imageSearchNoResults = imageSearchNoResults,
        imageDownloadError = imageDownloadError,
        onSelectSearchImage = onSelectSearchImage,
        onDismissImageSearch = onDismissImageSearch,
    )
}

/** Confirmation dialog shown before permanently deleting a trip. */
@Composable
private fun DeleteTripConfirmationDialog(
    tripTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_delete_trip_dialog_title)) },
        text = { Text(stringResource(R.string.home_delete_trip_dialog_message, tripTitle)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.home_delete_trip_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/**
 * Full-screen dialog that lets the user search for images online and pick one for their trip.
 *
 * Results are displayed in a 3-column thumbnail grid. Tapping a thumbnail triggers a download
 * and closes the dialog.
 */
@Composable
private fun ImageSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
    isDownloading: Boolean,
    results: List<ImageSearchResult>,
    hasError: Boolean,
    hasNoResults: Boolean,
    hasDownloadError: Boolean,
    onSelectImage: (ImageSearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.image_search_dialog_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = { Text(stringResource(R.string.image_search_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onSearch,
                        enabled = query.isNotBlank() && !isLoading && !isDownloading,
                    ) {
                        Text(stringResource(R.string.image_search_button))
                    }
                }
                if (hasDownloadError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.image_search_download_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    isDownloading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.image_search_downloading),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.image_search_loading),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    hasError -> {
                        Text(
                            text = stringResource(R.string.image_search_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    hasNoResults -> {
                        Text(
                            text = stringResource(R.string.image_search_no_results),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    results.isNotEmpty() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(results) { result ->
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(result.thumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = stringResource(
                                        R.string.image_search_result_desc,
                                        result.description,
                                    ),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { onSelectImage(result) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun TripsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.trips_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.trips_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


/**
 * An outlined text field that opens a timezone picker dialog when tapped.
 *
 * The device's current default timezone is shown as the placeholder when [selectedTimezone]
 * is `null`.  Selecting a timezone calls [onTimezoneChange] with the IANA zone ID string.
 */
@Composable
private fun TimezoneDropdown(
    selectedTimezone: String?,
    onTimezoneChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceDefault = remember { ZoneId.systemDefault().id }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val displayValue = selectedTimezone ?: stringResource(R.string.trip_timezone_device_default, deviceDefault)

    if (showPicker) {
        TimezonePickerDialog(
            onTimezoneSelected = { zoneId ->
                onTimezoneChange(zoneId)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.trip_timezone_label)) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .semantics {
                    role = Role.Button
                    contentDescription = displayValue
                }
                .clickable { showPicker = true },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    WanderVaultTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onAddTripClick = {},
            onDismissDialog = {},
            onTitleChange = {},
            onImageUriChange = {},
            onSaveTrip = {},
            onEditTripClick = {},
            onDismissEditDialog = {},
            onEditTitleChange = {},
            onEditImageUriChange = {},
            onUpdateTrip = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithTripsPreview() {
    val sampleTrips = listOf(
        Trip(1, "Paris Getaway", startDate = LocalDate.of(2024, 6, 1), endDate = LocalDate.of(2024, 6, 10)),
        Trip(2, "Tokyo Adventure"),
    )
    WanderVaultTheme {
        HomeScreenContent(
            uiState = HomeUiState(trips = sampleTrips),
            onAddTripClick = {},
            onDismissDialog = {},
            onTitleChange = {},
            onImageUriChange = {},
            onSaveTrip = {},
            onEditTripClick = {},
            onDismissEditDialog = {},
            onEditTitleChange = {},
            onEditImageUriChange = {},
            onUpdateTrip = {},
        )
    }
}

