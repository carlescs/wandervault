package cat.company.wandervault.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.ScannedDocumentInfo
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import java.io.File

/** Sub-directory of the app cache that holds the temporary camera capture. */
private const val SCAN_CACHE_DIR = "scans"

/** File name of the temporary camera capture (overwritten on every capture). */
private const val SCAN_CAPTURE_FILE = "scan_capture.jpg"

/**
 * Launches the camera or the system photo picker to obtain an image of a travel document.
 *
 * Camera captures are written to a single temporary file in the app cache, which is deleted by
 * [deleteCapture] once the scan has finished so that document photos are not kept around.
 */
internal class DocumentImagePicker(
    private val context: Context,
    private val onTakePhoto: (Uri) -> Unit,
    private val onPickImage: () -> Unit,
) {
    /** Opens the camera to photograph a document. */
    fun takePhoto() {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", captureFile(context))
        } catch (e: IllegalArgumentException) {
            showToast(R.string.scan_document_camera_unavailable)
            return
        }
        try {
            onTakePhoto(uri)
        } catch (e: ActivityNotFoundException) {
            showToast(R.string.scan_document_camera_unavailable)
        }
    }

    /** Opens the system photo picker to choose an existing image of a document. */
    fun pickImage() {
        try {
            onPickImage()
        } catch (e: ActivityNotFoundException) {
            showToast(R.string.scan_document_picker_unavailable)
        }
    }

    /** Deletes the temporary camera capture, if any. */
    fun deleteCapture() {
        captureFile(context, create = false).delete()
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun captureFile(context: Context, create: Boolean = true): File {
        val dir = File(context.cacheDir, SCAN_CACHE_DIR)
        if (create) dir.mkdirs()
        return File(dir, SCAN_CAPTURE_FILE)
    }
}

/**
 * Remembers a [DocumentImagePicker] that reports the chosen image URI through [onImageSelected].
 */
@Composable
internal fun rememberDocumentImagePicker(onImageSelected: (String) -> Unit): DocumentImagePicker {
    val context = LocalContext.current
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (saved && uri != null) onImageSelected(uri)
    }
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImageSelected(uri.toString())
    }
    return remember(context, takePictureLauncher, pickImageLauncher) {
        DocumentImagePicker(
            context = context,
            onTakePhoto = { uri ->
                pendingCaptureUri = uri.toString()
                takePictureLauncher.launch(uri)
            },
            onPickImage = {
                pickImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

/**
 * Top-app-bar action that offers to scan a document with the camera or from the gallery.
 */
@Composable
internal fun ScanDocumentAction(
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(
            imageVector = Icons.Default.DocumentScanner,
            contentDescription = stringResource(R.string.scan_document_action),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.scan_document_take_photo)) },
            leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
            onClick = {
                expanded = false
                onTakePhoto()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.scan_document_choose_image)) },
            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
            onClick = {
                expanded = false
                onPickImage()
            },
        )
    }
}

/**
 * Shows the progress / failure dialogs for [state]. Renders nothing for
 * [DocumentScanUiState.Idle] and [DocumentScanUiState.Review] (the review dialog is
 * screen-specific).
 */
@Composable
internal fun DocumentScanStatusDialogs(state: DocumentScanUiState, onDismiss: () -> Unit) {
    when (state) {
        DocumentScanUiState.Scanning -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.scan_document_title)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(R.string.scan_document_scanning),
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
        DocumentScanUiState.NothingFound -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.scan_document_title)) },
            text = { Text(stringResource(R.string.scan_document_nothing_found)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
            },
        )
        DocumentScanUiState.Error -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.scan_document_title)) },
            text = { Text(stringResource(R.string.scan_document_error)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
            },
        )
        DocumentScanUiState.Idle, is DocumentScanUiState.Review -> Unit
    }
}

/**
 * Review dialog for fields scanned from a boarding pass / ticket. The user can edit each field
 * and choose which leg to update (or create a new one) before anything is saved.
 *
 * @param info The scanned fields.
 * @param legs The current transport legs.
 * @param suggestedLegIndex The leg pre-selected in the dialog, or `null` for "new leg".
 * @param onConfirm Called with the selected leg index (`null` for a new leg), the flight number,
 *   the confirmation number and the company. Blank values leave the leg's field unchanged.
 * @param onDismiss Called when the user cancels; nothing is saved.
 */
@Composable
internal fun ScannedTransportReviewDialog(
    info: ScannedDocumentInfo,
    legs: List<TransportLegEditState>,
    suggestedLegIndex: Int?,
    onConfirm: (legIndex: Int?, flightNumber: String, confirmationNumber: String, company: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var flightNumber by rememberSaveable(info) { mutableStateOf(info.flightNumber.orEmpty()) }
    var confirmation by rememberSaveable(info) { mutableStateOf(info.bookingReference.orEmpty()) }
    var company by rememberSaveable(info) { mutableStateOf(info.carrier.orEmpty()) }
    // -1 represents "new leg" so the value can be saved in a Bundle.
    var selectedLeg by rememberSaveable(info) { mutableStateOf(suggestedLegIndex ?: -1) }
    val hasValues = flightNumber.isNotBlank() || confirmation.isNotBlank() || company.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_document_review_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.scan_document_review_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = flightNumber,
                    onValueChange = { flightNumber = it },
                    label = { Text(stringResource(R.string.transport_flight_number_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text(stringResource(R.string.transport_confirmation_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text(stringResource(R.string.transport_company_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.scan_document_apply_to_leg),
                    style = MaterialTheme.typography.titleSmall,
                )
                Column(modifier = Modifier.selectableGroup()) {
                    legs.forEachIndexed { index, leg ->
                        LegChoiceRow(
                            label = legChoiceLabel(index, leg),
                            selected = selectedLeg == index,
                            onClick = { selectedLeg = index },
                        )
                    }
                    LegChoiceRow(
                        label = stringResource(R.string.scan_document_new_leg),
                        selected = selectedLeg == -1,
                        onClick = { selectedLeg = -1 },
                    )
                }
                RecognizedTextSection(info.rawText)
                ScanPrivacyNote()
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        selectedLeg.takeIf { it in legs.indices },
                        flightNumber.trim(),
                        confirmation.trim(),
                        company.trim(),
                    )
                },
                enabled = hasValues,
            ) {
                Text(stringResource(R.string.scan_document_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/**
 * Review dialog for fields scanned from a hotel reservation. The user can edit each field
 * before it is applied to the destination's hotel.
 *
 * @param onConfirm Called with the hotel name, address and reservation number. Blank values
 *   leave the hotel's field unchanged.
 * @param onDismiss Called when the user cancels; nothing is saved.
 */
@Composable
internal fun ScannedHotelReviewDialog(
    info: ScannedDocumentInfo,
    onConfirm: (name: String, address: String, reservationNumber: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(info) { mutableStateOf(info.hotelName.orEmpty()) }
    var address by rememberSaveable(info) { mutableStateOf(info.hotelAddress.orEmpty()) }
    var reservation by rememberSaveable(info) { mutableStateOf(info.bookingReference.orEmpty()) }
    val hasValues = name.isNotBlank() || address.isNotBlank() || reservation.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_document_review_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.scan_document_review_hotel_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.hotel_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.hotel_address_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reservation,
                    onValueChange = { reservation = it },
                    label = { Text(stringResource(R.string.hotel_reservation_number_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                RecognizedTextSection(info.rawText)
                ScanPrivacyNote()
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), address.trim(), reservation.trim()) },
                enabled = hasValues,
            ) {
                Text(stringResource(R.string.scan_document_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun legChoiceLabel(index: Int, leg: TransportLegEditState): String {
    val parts = buildList {
        add(stringResource(R.string.transport_detail_leg_number, index + 1))
        leg.typeName
            ?.let { name -> runCatching { TransportType.valueOf(name) }.getOrNull() }
            ?.let { add(stringResource(it.detailLabelRes)) }
        leg.flightNumber.takeIf { it.isNotBlank() }?.let { add(it) }
        leg.company.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun LegChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Collapsible section showing the full text recognised in the image. */
@Composable
private fun RecognizedTextSection(rawText: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(
            stringResource(
                if (expanded) R.string.scan_document_hide_text else R.string.scan_document_show_text,
            ),
        )
    }
    if (expanded) {
        SelectionContainer(
            modifier = Modifier
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(text = rawText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Reassures the user that scanning happens on-device and nothing is saved without confirmation. */
@Composable
private fun ScanPrivacyNote() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.scan_document_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val previewScan = ScannedDocumentInfo(
    rawText = "BOARDING PASS\nFLIGHT VY1234\nBOOKING REF X7K2PQ\nHotel Arts Barcelona",
    flightNumber = "VY1234",
    bookingReference = "X7K2PQ",
    carrier = "Vueling Airlines",
    hotelName = "Hotel Arts Barcelona",
    hotelAddress = "Carrer de la Marina 19-21, Barcelona",
)

@Preview(showBackground = true)
@Composable
private fun ScannedTransportReviewDialogPreview() {
    WanderVaultTheme {
        ScannedTransportReviewDialog(
            info = previewScan,
            legs = listOf(
                TransportLegEditState(id = 1, typeName = TransportType.DRIVING.name, company = "Taxi"),
                TransportLegEditState(id = 2, typeName = TransportType.FLIGHT.name),
            ),
            suggestedLegIndex = 1,
            onConfirm = { _, _, _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannedHotelReviewDialogPreview() {
    WanderVaultTheme {
        ScannedHotelReviewDialog(info = previewScan, onConfirm = { _, _, _ -> }, onDismiss = {})
    }
}
