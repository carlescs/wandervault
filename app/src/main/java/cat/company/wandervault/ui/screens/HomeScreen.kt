package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.sharedTripCoverImage
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
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
        onSaveTrip = viewModel::onSaveTrip,
        onEditTripClick = viewModel::onEditTripClick,
        onDismissEditDialog = viewModel::onDismissEditTripDialog,
        onEditTitleChange = viewModel::onEditTripTitleChange,
        onEditImageUriChange = viewModel::onEditTripImageUriChange,
        onUpdateTrip = viewModel::onUpdateTrip,
        onDeleteTripClick = viewModel::onDeleteTripClick,
        onConfirmDeleteTrip = viewModel::onConfirmDeleteTrip,
        onDismissDeleteDialog = viewModel::onDismissDeleteTripDialog,
        onFavoriteClick = viewModel::onToggleFavorite,
        onTripClick = onTripClick,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the home screen.
 *
 * Accepts a [HomeUiState] snapshot and event callbacks so it can be reused
 * in `@Preview` functions without a real [HomeViewModel].
 */
@Composable
internal fun HomeScreenContent(
    uiState: HomeUiState,
    onAddTripClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onTitleChange: (String) -> Unit,
    onImageUriChange: (String?) -> Unit,
    onSaveTrip: () -> Unit,
    onEditTripClick: (Trip) -> Unit,
    onDismissEditDialog: () -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditImageUriChange: (String?) -> Unit,
    onUpdateTrip: () -> Unit,
    onDeleteTripClick: (Trip) -> Unit = {},
    onConfirmDeleteTrip: () -> Unit = {},
    onDismissDeleteDialog: () -> Unit = {},
    onFavoriteClick: (Trip) -> Unit = {},
    onTripClick: (Int) -> Unit = {},
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
                items(uiState.trips) { trip ->
                    TripCard(
                        trip = trip,
                        onEditClick = { onEditTripClick(trip) },
                        onDeleteClick = { onDeleteTripClick(trip) },
                        onFavoriteClick = { onFavoriteClick(trip) },
                        onCardClick = { onTripClick(trip.id) },
                    )
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
            isFormValid = uiState.isAddTripFormValid,
            onSave = onSaveTrip,
            onDismiss = onDismissDialog,
        )
    }

    if (uiState.showEditTripDialog) {
        EditTripDialog(
            title = uiState.editTripTitle,
            onTitleChange = onEditTitleChange,
            imageUri = uiState.editTripImageUri,
            onImageUriChange = onEditImageUriChange,
            isFormValid = uiState.isEditTripFormValid,
            onSave = onUpdateTrip,
            onDismiss = onDismissEditDialog,
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
private fun TripCard(trip: Trip, onEditClick: () -> Unit, onDeleteClick: () -> Unit = {}, onFavoriteClick: () -> Unit = {}, onCardClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Card(modifier = modifier.fillMaxWidth(), onClick = onCardClick) {
        if (trip.imageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(trip.imageUri))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.trip_image_content_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .sharedTripCoverImage(trip.id),
            )
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
 * @param isFormValid Whether the form inputs are valid, enabling the save button.
 * @param onSave Called when the user confirms the dialog.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 */
@Composable
private fun TripFormDialog(
    dialogTitle: String,
    title: String,
    onTitleChange: (String) -> Unit,
    imageUri: String?,
    onImageUriChange: (String?) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        onImageUriChange(uri?.toString())
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
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    TripFormDialog(
        dialogTitle = stringResource(R.string.add_trip_title),
        title = title,
        onTitleChange = onTitleChange,
        imageUri = imageUri,
        onImageUriChange = onImageUriChange,
        isFormValid = isFormValid,
        onSave = onSave,
        onDismiss = onDismiss,
    )
}

@Composable
private fun EditTripDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    imageUri: String?,
    onImageUriChange: (String?) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    TripFormDialog(
        dialogTitle = stringResource(R.string.edit_trip_title),
        title = title,
        onTitleChange = onTitleChange,
        imageUri = imageUri,
        onImageUriChange = onImageUriChange,
        isFormValid = isFormValid,
        onSave = onSave,
        onDismiss = onDismiss,
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

