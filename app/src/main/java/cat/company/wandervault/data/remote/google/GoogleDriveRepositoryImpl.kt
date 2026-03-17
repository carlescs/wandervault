package cat.company.wandervault.data.remote.google

import android.content.Context
import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.repository.GoogleDriveRepository

private const val PREFS_NAME = "wandervault_prefs"
private const val KEY_DRIVE_SIGNED_IN = "drive_signed_in"
private const val KEY_DRIVE_FOLDER_ID = "drive_folder_id"
private const val KEY_DRIVE_FOLDER_NAME = "drive_folder_name"

/**
 * Stub implementation of [GoogleDriveRepository].
 *
 * Sign-in state and the selected folder are persisted in SharedPreferences.
 * The actual Google Drive API calls (OAuth flow, file upload) are left for a concrete
 * integration layer that supplies real Google credentials; this class provides all the
 * plumbing so the rest of the app can be fully wired up and tested without them.
 *
 * To replace this stub with a real implementation:
 *  1. Add the `com.google.android.gms:play-services-auth` and Google Drive API
 *     dependencies to `app/build.gradle.kts`.
 *  2. Configure an OAuth 2.0 client ID in the Google Cloud Console and add the
 *     `google-services.json` file to the app module.
 *  3. Implement [signIn] using `GoogleSignInClient` / `AuthorizationClient` with
 *     `DriveScopes.DRIVE_FILE` scope.
 *  4. Implement [uploadFile] using the Drive REST v3 API or the Drive Android SDK.
 */
class GoogleDriveRepositoryImpl(context: Context) : GoogleDriveRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isSignedIn(): Boolean = prefs.getBoolean(KEY_DRIVE_SIGNED_IN, false)

    override suspend fun signIn(): Result<Unit> {
        // TODO: Replace with real Google Sign-In / OAuth flow using GoogleSignInClient.
        // The scope required is DriveScopes.DRIVE_FILE (or DRIVE for full access).
        // On success, persist the sign-in state and refresh token here.
        prefs.edit().putBoolean(KEY_DRIVE_SIGNED_IN, true).apply()
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        prefs.edit()
            .remove(KEY_DRIVE_SIGNED_IN)
            .remove(KEY_DRIVE_FOLDER_ID)
            .remove(KEY_DRIVE_FOLDER_NAME)
            .apply()
    }

    override fun getSelectedFolderId(): String? = prefs.getString(KEY_DRIVE_FOLDER_ID, null)

    override fun getSelectedFolderName(): String? = prefs.getString(KEY_DRIVE_FOLDER_NAME, null)

    override fun setSelectedFolder(folderId: String?, folderName: String?) {
        prefs.edit().apply {
            if (folderId == null) {
                remove(KEY_DRIVE_FOLDER_ID)
                remove(KEY_DRIVE_FOLDER_NAME)
            } else {
                putString(KEY_DRIVE_FOLDER_ID, folderId)
                putString(KEY_DRIVE_FOLDER_NAME, folderName)
            }
            apply()
        }
    }

    override suspend fun listFolders(): Result<List<DriveFolder>> {
        // TODO: Replace with real Drive API call:
        //   drive.files().list()
        //       .setQ("mimeType='application/vnd.google-apps.folder' and trashed=false")
        //       .setSpaces("drive")
        //       .execute()
        return Result.success(emptyList())
    }

    override suspend fun uploadFile(
        localUri: String,
        mimeType: String,
        fileName: String,
        remotePath: List<String>,
    ): Result<String> {
        // TODO: Replace with real Drive API upload:
        //  1. Resolve or create the folder hierarchy described by remotePath under the
        //     selected root folder (getSelectedFolderId()).
        //  2. Upload the file using a MediaHttpUploader with the resolved folder as parent.
        //  3. Return the Drive file ID of the newly created file.
        return Result.failure(UnsupportedOperationException("Drive upload not yet implemented"))
    }
}
