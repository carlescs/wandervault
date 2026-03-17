package cat.company.wandervault.domain.model

/**
 * Represents a Google Drive folder that the user can select as the upload destination.
 *
 * @param id Unique Google Drive folder ID.
 * @param name Display name of the folder.
 */
data class DriveFolder(
    val id: String,
    val name: String,
)
