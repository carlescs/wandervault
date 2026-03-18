package cat.company.wandervault

import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.repository.GoogleDriveRepository
import cat.company.wandervault.domain.usecase.GetDriveSignInStatusUseCase
import cat.company.wandervault.domain.usecase.GetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.ListDriveFoldersUseCase
import cat.company.wandervault.domain.usecase.SetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.SignInToDriveUseCase
import cat.company.wandervault.domain.usecase.SignOutFromDriveUseCase
import cat.company.wandervault.domain.usecase.UploadDocumentToDriveUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Google Drive use-cases.
 *
 * Uses a [FakeGoogleDriveRepository] stub to avoid network/platform dependencies.
 * Interactive sign-in intent construction and result parsing ([DriveSignInClient]) are
 * Android-specific concerns handled by the data layer and are NOT exercised here.
 */
class GoogleDriveUseCaseTest {

    private val repository = FakeGoogleDriveRepository()

    // ── GetDriveSignInStatusUseCase ──────────────────────────────────────────

    @Test
    fun `GetDriveSignInStatusUseCase returns false when not signed in`() {
        repository.signedIn = false
        assertFalse(GetDriveSignInStatusUseCase(repository)())
    }

    @Test
    fun `GetDriveSignInStatusUseCase returns true when signed in`() {
        repository.signedIn = true
        assertTrue(GetDriveSignInStatusUseCase(repository)())
    }

    // ── SignInToDriveUseCase ─────────────────────────────────────────────────

    @Test
    fun `SignInToDriveUseCase marks repository as signed in on success`() = runTest {
        repository.signedIn = false
        val result = SignInToDriveUseCase(repository)()
        assertTrue(result.isSuccess)
        assertTrue(repository.signedIn)
    }

    @Test
    fun `SignInToDriveUseCase returns failure when repository throws`() = runTest {
        repository.signInShouldFail = true
        val result = SignInToDriveUseCase(repository)()
        assertTrue(result.isFailure)
        assertFalse(repository.signedIn)
    }

    // ── SignOutFromDriveUseCase ──────────────────────────────────────────────

    @Test
    fun `SignOutFromDriveUseCase clears sign-in state`() = runTest {
        repository.signedIn = true
        SignOutFromDriveUseCase(repository)()
        assertFalse(repository.signedIn)
    }

    // ── GetSelectedDriveFolderUseCase ────────────────────────────────────────

    @Test
    fun `GetSelectedDriveFolderUseCase returns null when no folder selected`() {
        repository.selectedFolderId = null
        repository.selectedFolderName = null
        assertNull(GetSelectedDriveFolderUseCase(repository)())
    }

    @Test
    fun `GetSelectedDriveFolderUseCase returns DriveFolder when folder is selected`() {
        repository.selectedFolderId = "folder123"
        repository.selectedFolderName = "My Trips"
        val result = GetSelectedDriveFolderUseCase(repository)()
        assertEquals(DriveFolder(id = "folder123", name = "My Trips"), result)
    }

    // ── SetSelectedDriveFolderUseCase ────────────────────────────────────────

    @Test
    fun `SetSelectedDriveFolderUseCase persists folder selection`() {
        val folder = DriveFolder(id = "abc", name = "Travel Docs")
        SetSelectedDriveFolderUseCase(repository)(folder)
        assertEquals("abc", repository.selectedFolderId)
        assertEquals("Travel Docs", repository.selectedFolderName)
    }

    @Test
    fun `SetSelectedDriveFolderUseCase clears selection when null passed`() {
        repository.selectedFolderId = "existing"
        repository.selectedFolderName = "Old Folder"
        SetSelectedDriveFolderUseCase(repository)(null)
        assertNull(repository.selectedFolderId)
        assertNull(repository.selectedFolderName)
    }

    // ── ListDriveFoldersUseCase ──────────────────────────────────────────────

    @Test
    fun `ListDriveFoldersUseCase returns folders from repository`() = runTest {
        val folders = listOf(DriveFolder("1", "Folder A"), DriveFolder("2", "Folder B"))
        repository.foldersToReturn = folders
        val result = ListDriveFoldersUseCase(repository)()
        assertTrue(result.isSuccess)
        assertEquals(folders, result.getOrThrow())
    }

    // ── UploadDocumentToDriveUseCase ─────────────────────────────────────────

    @Test
    fun `UploadDocumentToDriveUseCase fails when not signed in`() = runTest {
        repository.signedIn = false
        val result = UploadDocumentToDriveUseCase(repository)(
            localUri = "file://doc.pdf",
            mimeType = "application/pdf",
            fileName = "doc.pdf",
            remotePath = listOf("Trip 2025"),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `UploadDocumentToDriveUseCase fails when no folder selected`() = runTest {
        repository.signedIn = true
        repository.selectedFolderId = null
        val result = UploadDocumentToDriveUseCase(repository)(
            localUri = "file://doc.pdf",
            mimeType = "application/pdf",
            fileName = "doc.pdf",
            remotePath = listOf("Trip 2025"),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `UploadDocumentToDriveUseCase delegates to repository when signed in and folder selected`() = runTest {
        repository.signedIn = true
        repository.selectedFolderId = "rootFolder"
        repository.uploadFileIdToReturn = "driveFile123"
        val result = UploadDocumentToDriveUseCase(repository)(
            localUri = "file://doc.pdf",
            mimeType = "application/pdf",
            fileName = "doc.pdf",
            remotePath = listOf("Trip 2025", "Flights"),
        )
        assertTrue(result.isSuccess)
        assertEquals("driveFile123", result.getOrThrow())
    }
}

// ── Fake implementation ───────────────────────────────────────────────────────

private class FakeGoogleDriveRepository : GoogleDriveRepository {

    var signedIn = false
    var signInShouldFail = false
    var selectedFolderId: String? = null
    var selectedFolderName: String? = null
    var foldersToReturn: List<DriveFolder> = emptyList()
    var uploadFileIdToReturn: String = "fileId"

    override fun isSignedIn(): Boolean = signedIn

    override suspend fun signIn(): Result<Unit> {
        if (signInShouldFail) return Result.failure(RuntimeException("Sign-in failed"))
        signedIn = true
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        signedIn = false
        selectedFolderId = null
        selectedFolderName = null
    }

    override fun getSelectedFolderId(): String? = selectedFolderId

    override fun getSelectedFolderName(): String? = selectedFolderName

    override fun setSelectedFolder(folderId: String?, folderName: String?) {
        selectedFolderId = folderId
        selectedFolderName = folderName
    }

    override suspend fun listFolders(): Result<List<DriveFolder>> =
        Result.success(foldersToReturn)

    override suspend fun uploadFile(
        localUri: String,
        mimeType: String,
        fileName: String,
        remotePath: List<String>,
    ): Result<String> = Result.success(uploadFileIdToReturn)
}
