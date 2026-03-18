package cat.company.wandervault.domain.repository

import android.content.Intent
import cat.company.wandervault.domain.model.DriveFolder

/**
 * Repository for Google Drive operations.
 *
 * Handles authentication, folder listing, and file uploads.  Implementations are
 * expected to use OAuth 2.0 (via Google Sign-In) and the Drive REST API.
 */
interface GoogleDriveRepository {

    /** Returns `true` if the user is currently signed in to their Google account. */
    fun isSignedIn(): Boolean

    /**
     * Attempts a silent (no-UI) sign-in using a previously granted account.
     *
     * Returns [Result.success] when the account is already authorised for the Drive scope.
     * Returns [Result.failure] when no cached account exists or it lacks the required scope;
     * in that case the caller should obtain an explicit sign-in intent via [buildSignInIntent]
     * and handle the result with [handleSignInResult].
     */
    suspend fun signIn(): Result<Unit>

    /**
     * Builds the [Intent] that launches the Google Sign-In activity.
     *
     * This is only needed when [signIn] returns a failure (i.e. there is no cached account).
     * Launch this intent with [ActivityResultLauncher] and pass the result data to
     * [handleSignInResult].
     */
    fun buildSignInIntent(): Intent

    /**
     * Processes the [Intent] returned from the Google Sign-In activity.
     *
     * @param data The result [Intent] from the sign-in activity, or `null` if the user
     *   cancelled.
     * @return [Result.success] on successful sign-in, [Result.failure] otherwise.
     */
    suspend fun handleSignInResult(data: Intent?): Result<Unit>

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
     * Lists the top-level Drive folders accessible to the signed-in user.
     *
     * @return [Result.success] with the list of folders, or [Result.failure] with the
     *   underlying exception.
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
