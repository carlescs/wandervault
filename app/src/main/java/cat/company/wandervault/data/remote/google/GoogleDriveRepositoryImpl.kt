package cat.company.wandervault.data.remote.google

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.repository.GoogleDriveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "wandervault_prefs"
private const val KEY_DRIVE_SIGNED_IN = "drive_signed_in"
private const val KEY_DRIVE_FOLDER_ID = "drive_folder_id"
private const val KEY_DRIVE_FOLDER_NAME = "drive_folder_name"
private const val APP_NAME = "WanderVault"
private const val DRIVE_FOLDER_MIME = "application/vnd.google-apps.folder"

/**
 * Production implementation of [GoogleDriveRepository].
 *
 * Sign-in uses the Google Sign-In SDK ([GoogleSignInClient]) requesting the
 * [DriveScopes.DRIVE_FILE] OAuth scope so that the app can create and upload files.
 * File-system operations use the Drive REST v3 API via [Drive].
 *
 * **Configuration**: An Android OAuth 2.0 client ID must be registered in the
 * Google Cloud Console for this app's package name and signing certificate SHA-1.
 * A `google-services.json` file is not strictly required for Sign-In — the GMS SDK
 * resolves the client ID automatically from the registered package name + SHA-1.
 */
class GoogleDriveRepositoryImpl(private val context: Context) : GoogleDriveRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val signInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    /** Cached [Drive] service; rebuilt after each successful sign-in. */
    @Volatile
    private var driveService: Drive? = null

    // ── Authentication ────────────────────────────────────────────────────────

    override fun isSignedIn(): Boolean = prefs.getBoolean(KEY_DRIVE_SIGNED_IN, false)

    /**
     * Attempts a silent sign-in using a cached account that already has the Drive scope.
     * Returns [Result.failure] when interactive sign-in is required; the caller should
     * then use [buildSignInIntent] + [handleSignInResult].
     */
    override suspend fun signIn(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null &&
                GoogleSignIn.hasPermissions(lastAccount, Scope(DriveScopes.DRIVE_FILE))
            ) {
                persistSignIn()
                return@withContext Result.success(Unit)
            }
            Tasks.await(googleSignInClient.silentSignIn())
            persistSignIn()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun buildSignInIntent(): Intent = googleSignInClient.signInIntent

    override suspend fun handleSignInResult(data: Intent?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                // Verify the account is non-null; credentials are retrieved lazily from
                // GoogleSignIn.getLastSignedInAccount() inside requireDriveService().
                task.getResult(ApiException::class.java)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Sign-in cancelled or returned null account"),
                    )
                persistSignIn()
                Result.success(Unit)
            } catch (e: ApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) { Tasks.await(googleSignInClient.signOut()) }
        driveService = null
        prefs.edit()
            .remove(KEY_DRIVE_SIGNED_IN)
            .remove(KEY_DRIVE_FOLDER_ID)
            .remove(KEY_DRIVE_FOLDER_NAME)
            .apply()
    }

    // ── Folder selection ──────────────────────────────────────────────────────

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

    // ── Drive API calls ───────────────────────────────────────────────────────

    override suspend fun listFolders(): Result<List<DriveFolder>> = withContext(Dispatchers.IO) {
        try {
            val service = requireDriveService()
            val result = service.files().list()
                .setQ("mimeType='$DRIVE_FOLDER_MIME' and trashed=false")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .execute()
            val folders = result.files
                ?.map { DriveFolder(id = it.id, name = it.name) }
                ?: emptyList()
            Result.success(folders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(
        localUri: String,
        mimeType: String,
        fileName: String,
        remotePath: List<String>,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = requireDriveService()
            val rootFolderId = getSelectedFolderId()
                ?: return@withContext Result.failure(
                    IllegalStateException("No Drive folder selected"),
                )
            val parentFolderId = resolveFolderPath(service, rootFolderId, remotePath)
            val inputStream = context.contentResolver.openInputStream(Uri.parse(localUri))
                ?: return@withContext Result.failure(
                    IllegalStateException("Cannot open local file: $localUri"),
                )
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(parentFolderId)
            }
            val mediaContent = InputStreamContent(mimeType, inputStream)
            val uploaded = inputStream.use {
                service.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            }
            Result.success(uploaded.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a [Drive] service for the currently signed-in account.
     *
     * @throws IllegalStateException if the user is not signed in or the account lacks
     *   the Drive scope.
     */
    private fun requireDriveService(): Drive {
        driveService?.let { return it }
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("Not signed in to Google Drive")
        if (!GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            throw IllegalStateException("Drive permission not granted")
        }
        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
            .setSelectedAccount(account.account)
        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName(APP_NAME)
            .build()
            .also { driveService = it }
    }

    /** Updates SharedPreferences and invalidates the cached [Drive] service. */
    private fun persistSignIn() {
        driveService = null
        prefs.edit().putBoolean(KEY_DRIVE_SIGNED_IN, true).apply()
    }

    /**
     * Walks [path] under [rootFolderId], creating any missing sub-folders along the way,
     * and returns the Drive folder ID of the deepest folder in the path.
     */
    private fun resolveFolderPath(
        service: Drive,
        rootFolderId: String,
        path: List<String>,
    ): String {
        var currentId = rootFolderId
        for (name in path) {
            currentId = findOrCreateFolder(service, currentId, name)
        }
        return currentId
    }

    /**
     * Returns the ID of an existing folder named [name] whose parent is [parentId], or
     * creates it and returns its new ID.
     */
    private fun findOrCreateFolder(service: Drive, parentId: String, name: String): String {
        // Drive query syntax uses '' (two single quotes) to escape an embedded single quote.
        val escapedName = name.replace("'", "''")
        val existing = service.files().list()
            .setQ(
                "mimeType='$DRIVE_FOLDER_MIME'" +
                    " and name='$escapedName'" +
                    " and '$parentId' in parents" +
                    " and trashed=false",
            )
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
            .files
            ?.firstOrNull()
        if (existing != null) return existing.id

        val metadata = File().apply {
            this.name = name
            mimeType = DRIVE_FOLDER_MIME
            parents = listOf(parentId)
        }
        return service.files().create(metadata).setFields("id").execute().id
    }
}
