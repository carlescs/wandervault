package cat.company.wandervault.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.TripDocument
import java.io.File

/**
 * Opens a [TripDocument] using the system's [Intent.ACTION_VIEW] intent.
 *
 * For `file://` URIs the file is first re-wrapped via [FileProvider] so that the receiving app
 * gets a content URI with read permission. All other URI schemes (content://, etc.) are used
 * directly.
 *
 * Shows a short [Toast] if no app is available to open the document or if the URI is malformed.
 */
internal fun openTripDocument(context: Context, document: TripDocument) {
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
        Toast.makeText(context, context.getString(R.string.documents_no_app_to_open), Toast.LENGTH_SHORT).show()
    } catch (_: IllegalArgumentException) {
        Toast.makeText(context, context.getString(R.string.documents_no_app_to_open), Toast.LENGTH_SHORT).show()
    }
}
