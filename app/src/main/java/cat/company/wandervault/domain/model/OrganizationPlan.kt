package cat.company.wandervault.domain.model

/**
 * An AI-suggested folder organisation for a set of trip documents.
 *
 * @param folderAssignments Folders to create and the documents to place in each.
 *   Documents that the AI did not assign to any folder are not included.
 */
data class OrganizationPlan(
    val folderAssignments: List<FolderAssignment>,
)

/**
 * A single folder suggested by the auto-organize AI, together with the documents to place in it.
 *
 * @param folderName Display name for the folder to create.
 * @param documents Documents that the AI recommends placing in this folder.
 */
data class FolderAssignment(
    val folderName: String,
    val documents: List<TripDocument>,
)
