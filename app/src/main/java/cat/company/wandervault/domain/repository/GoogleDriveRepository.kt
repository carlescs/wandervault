package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.DriveFolder

/**
 * Repository for Google Drive operations.
 *
 * Handles authentication, folder listing, and file uploads.  Implementations are
 * expected to use OAuth 2.0 (via Google Sign-In) and the Drive REST API.
 *
 * Interactive sign-in intent construction and result parsing are deliberately kept
 * out of this interface to avoid coupling the domain layer to Android platform types.
 * Those concerns are handled by [cat.company.wandervault.data.remote.google.DriveSignInClient].
 */
interface GoogleDriveRepository {

    /** Returns `true` if the user is currently signed in to their Google account. */
    fun isSignedIn(): Boolean

    /**
     * Attempts a silent (no-UI) sign-in using a previously granted account.
     *
     * Returns [Result.success] when the account is already authorised for the Drive scope.
     * Returns [Result.failure] when no cached account exists or it lacks the required scope;
     * in that case the caller should use
     * [cat.company.wandervault.data.remote.google.DriveSignInClient.buildSignInIntent] to
     * start the interactive sign-in activity.
     */
    suspend fun signIn(): Result<Unit>

    /** Signs the user out of their Google account and clears any cached credentials. */
    suspend fun signOut()

    /**
     * Returns the Drive folder ID that the user has selected as the upload destination,
     * or `null` if no folder has been selected.
     */
    fun getSelectedFolderId(): String?

    /**
     * Returns the Drive folder name that the user has selected as the upload destination,
     * or `null` if no folder has been selected.
     */
    fun getSelectedFolderName(): String?

    /**
     * Persists [folderId] and [folderName] as the user's chosen Drive upload folder.
     * Pass `null` for both to clear the selection.
     */
    fun setSelectedFolder(folderId: String?, folderName: String?)

    /**
     * Lists the top-level Drive folders (direct children of the Drive root) accessible
     * to the signed-in user.
     *
     * @return [Result.success] with the complete list of top-level folders, or
     *   [Result.failure] with the underlying exception.
     */
    suspend fun listFolders(): Result<List<DriveFolder>>

    /**
     * Uploads [localUri] to the selected Drive folder under [remotePath].
     *
     * The [remotePath] mirrors the local trip/folder hierarchy so the Drive structure
     * matches the on-device layout.
     *
     * @param localUri  URI of the local file to upload.
     * @param mimeType  MIME type of the file (e.g. "application/pdf").
     * @param fileName  Name to give the file in Drive.
     * @param remotePath Path segments describing the folder hierarchy in Drive.
     * @return [Result.success] with the Drive file ID, or [Result.failure].
     */
    suspend fun uploadFile(
        localUri: String,
        mimeType: String,
        fileName: String,
        remotePath: List<String>,
    ): Result<String>
}
