package cat.company.wandervault

import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.DocumentSummaryRepository
import cat.company.wandervault.domain.repository.TripDocumentRepository
import cat.company.wandervault.domain.usecase.CopyDocumentToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.DeleteDocumentUseCase
import cat.company.wandervault.domain.usecase.DeleteFolderUseCase
import cat.company.wandervault.domain.usecase.GetDocumentByIdUseCase
import cat.company.wandervault.domain.usecase.GetDocumentsInFolderUseCase
import cat.company.wandervault.domain.usecase.GetRootDocumentsUseCase
import cat.company.wandervault.domain.usecase.GetRootFoldersUseCase
import cat.company.wandervault.domain.usecase.GetSubFoldersUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.SuggestDocumentNameUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for all TripDocument-related use-cases.
 *
 * Uses a [FakeTripDocumentRepository] stub to avoid Room dependencies.
 */
class TripDocumentUseCaseTest {

    private val repository = FakeTripDocumentRepository()

    // ── GetRootFoldersUseCase ─────────────────────────────────────────────────

    @Test
    fun `GetRootFoldersUseCase returns root folders for trip`() = runTest {
        val folder = TripDocumentFolder(id = 1, tripId = 42, name = "Tickets")
        repository.rootFolders[42] = mutableListOf(folder)

        val result = GetRootFoldersUseCase(repository)(42).first()

        assertEquals(listOf(folder), result)
    }

    // ── GetDocumentByIdUseCase ────────────────────────────────────────────────

    @Test
    fun `GetDocumentByIdUseCase returns matching document`() = runTest {
        val doc = TripDocument(id = 7, tripId = 1, folderId = 2, name = "ticket.pdf", uri = "uri", mimeType = "application/pdf")
        repository.documents[2] = mutableListOf(doc)

        val result = GetDocumentByIdUseCase(repository)(7).first()

        assertEquals(doc, result)
    }

    @Test
    fun `GetDocumentByIdUseCase returns null when document not found`() = runTest {
        val result = GetDocumentByIdUseCase(repository)(999).first()

        assertNull(result)
    }

    // ── GetSubFoldersUseCase ──────────────────────────────────────────────────

    @Test
    fun `GetSubFoldersUseCase returns sub-folders for parent folder`() = runTest {
        val sub = TripDocumentFolder(id = 2, tripId = 1, name = "Sub", parentFolderId = 10)
        repository.subFolders[10] = mutableListOf(sub)

        val result = GetSubFoldersUseCase(repository)(10).first()

        assertEquals(listOf(sub), result)
    }

    // ── GetDocumentsInFolderUseCase ───────────────────────────────────────────

    @Test
    fun `GetDocumentsInFolderUseCase returns documents for folder`() = runTest {
        val doc = TripDocument(id = 1, tripId = 1, folderId = 5, name = "boarding.pdf", uri = "uri", mimeType = "application/pdf")
        repository.documents[5] = mutableListOf(doc)

        val result = GetDocumentsInFolderUseCase(repository)(5).first()

        assertEquals(listOf(doc), result)
    }

    // ── GetRootDocumentsUseCase ───────────────────────────────────────────────

    @Test
    fun `GetRootDocumentsUseCase returns root-level documents for trip`() = runTest {
        val doc = TripDocument(id = 1, tripId = 7, name = "passport.pdf", uri = "uri", mimeType = "application/pdf")
        repository.rootDocuments[7] = mutableListOf(doc)

        val result = GetRootDocumentsUseCase(repository)(7).first()

        assertEquals(listOf(doc), result)
    }

    // ── SaveFolderUseCase ─────────────────────────────────────────────────────

    @Test
    fun `SaveFolderUseCase delegates to repository`() = runTest {
        val folder = TripDocumentFolder(tripId = 1, name = "Hotels")

        SaveFolderUseCase(repository)(folder)

        assertTrue(repository.savedFolders.contains(folder))
    }

    // ── UpdateFolderUseCase ───────────────────────────────────────────────────

    @Test
    fun `UpdateFolderUseCase delegates to repository`() = runTest {
        val folder = TripDocumentFolder(id = 3, tripId = 1, name = "Renamed")

        UpdateFolderUseCase(repository)(folder)

        assertTrue(repository.updatedFolders.contains(folder))
    }

    // ── DeleteFolderUseCase ───────────────────────────────────────────────────

    @Test
    fun `DeleteFolderUseCase delegates to repository`() = runTest {
        val folder = TripDocumentFolder(id = 4, tripId = 1, name = "OldFolder")

        DeleteFolderUseCase(repository)(folder)

        assertTrue(repository.deletedFolders.contains(folder))
    }

    // ── SaveDocumentUseCase ───────────────────────────────────────────────────

    @Test
    fun `SaveDocumentUseCase delegates to repository`() = runTest {
        val doc = TripDocument(tripId = 1, folderId = 1, name = "ticket.pdf", uri = "uri", mimeType = "application/pdf")

        SaveDocumentUseCase(repository)(doc)

        assertTrue(repository.savedDocuments.contains(doc))
    }

    @Test
    fun `SaveDocumentUseCase delegates to repository for root-level document`() = runTest {
        val doc = TripDocument(tripId = 1, name = "passport.pdf", uri = "uri", mimeType = "application/pdf")

        SaveDocumentUseCase(repository)(doc)

        assertTrue(repository.savedDocuments.contains(doc))
    }

    // ── UpdateDocumentUseCase ─────────────────────────────────────────────────

    @Test
    fun `UpdateDocumentUseCase delegates to repository`() = runTest {
        val doc = TripDocument(id = 2, tripId = 1, folderId = 1, name = "renamed.pdf", uri = "uri", mimeType = "application/pdf")

        UpdateDocumentUseCase(repository)(doc)

        assertTrue(repository.updatedDocuments.contains(doc))
    }

    // ── DeleteDocumentUseCase ─────────────────────────────────────────────────

    @Test
    fun `DeleteDocumentUseCase delegates to repository`() = runTest {
        val doc = TripDocument(id = 3, tripId = 1, folderId = 1, name = "old.pdf", uri = "uri", mimeType = "application/pdf")

        DeleteDocumentUseCase(repository)(doc)

        assertTrue(repository.deletedDocuments.contains(doc))
    }

    // ── CopyDocumentToInternalStorageUseCase ──────────────────────────────────

    @Test
    fun `CopyDocumentToInternalStorageUseCase delegates to repository`() = runTest {
        val sourceUri = "content://com.example/document/42"

        val result = CopyDocumentToInternalStorageUseCase(repository)(sourceUri)

        assertEquals("file:///fake/documents/copy.pdf", result)
    }

    // ── SummarizeDocumentUseCase ──────────────────────────────────────────────

    @Test
    fun `SummarizeDocumentUseCase returns extraction result from repository`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(
            DocumentExtractionResult(
                summary = "Flight booking confirmation for Paris trip.",
                relevantTripInfo = "Dates: 2025-06-01 to 2025-06-10. Destination: Paris.",
            ),
        )

        val result = SummarizeDocumentUseCase(fakeRepo)(
            "file:///documents/booking.txt",
            "text/plain",
        )

        assertEquals("Flight booking confirmation for Paris trip.", result?.summary)
        assertEquals("Dates: 2025-06-01 to 2025-06-10. Destination: Paris.", result?.relevantTripInfo)
    }

    @Test
    fun `SummarizeDocumentUseCase returns null when repository returns null`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(null)

        val result = SummarizeDocumentUseCase(fakeRepo)(
            "file:///documents/photo.jpg",
            "image/jpeg",
        )

        assertNull(result)
    }

    @Test
    fun `SummarizeDocumentUseCase forwards tripYear to repository`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(
            DocumentExtractionResult(summary = "Hotel confirmation."),
        )

        SummarizeDocumentUseCase(fakeRepo)(
            "file:///documents/hotel.pdf",
            "application/pdf",
            tripYear = 2026,
        )

        assertEquals(2026, fakeRepo.lastTripYear)
    }

    @Test
    fun `SummarizeDocumentUseCase forwards null tripYear to repository when not provided`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(
            DocumentExtractionResult(summary = "Some document."),
        )

        val result = SummarizeDocumentUseCase(fakeRepo)(
            "file:///documents/misc.txt",
            "text/plain",
        )

        assertEquals("Some document.", result?.summary)
        assertNull(fakeRepo.lastTripYear)
    }

    // ── SuggestDocumentNameUseCase ────────────────────────────────────────────

    @Test
    fun `SuggestDocumentNameUseCase returns suggested name from repository`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(
            null,
            suggestedName = "Paris Flight Ticket",
        )

        val result = SuggestDocumentNameUseCase(fakeRepo)(
            "file:///documents/ticket.pdf",
            "application/pdf",
        )

        assertEquals("Paris Flight Ticket", result)
    }

    @Test
    fun `SuggestDocumentNameUseCase returns null when repository returns null`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(null, suggestedName = null)

        val result = SuggestDocumentNameUseCase(fakeRepo)(
            "file:///documents/photo.jpg",
            "image/jpeg",
        )

        assertNull(result)
    }

    // ── isAvailable delegation ────────────────────────────────────────────────

    @Test
    fun `SummarizeDocumentUseCase isAvailable returns true when repository is available`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(null, available = true)
        assertTrue(SummarizeDocumentUseCase(fakeRepo).isAvailable())
    }

    @Test
    fun `SummarizeDocumentUseCase isAvailable returns false when repository is unavailable`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(null, available = false)
        assertFalse(SummarizeDocumentUseCase(fakeRepo).isAvailable())
    }

    @Test
    fun `SuggestDocumentNameUseCase isAvailable returns true when repository is available`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(null, available = true)
        assertTrue(SuggestDocumentNameUseCase(fakeRepo).isAvailable())
    }

    @Test
    fun `SuggestDocumentNameUseCase isAvailable returns false when repository is unavailable`() = runTest {
        val fakeRepo = FakeDocumentSummaryRepository(null, available = false)
        assertFalse(SuggestDocumentNameUseCase(fakeRepo).isAvailable())
    }
}

private class FakeTripDocumentRepository : TripDocumentRepository {
    val rootFolders: MutableMap<Int, MutableList<TripDocumentFolder>> = mutableMapOf()
    val subFolders: MutableMap<Int, MutableList<TripDocumentFolder>> = mutableMapOf()
    val documents: MutableMap<Int, MutableList<TripDocument>> = mutableMapOf()
    val rootDocuments: MutableMap<Int, MutableList<TripDocument>> = mutableMapOf()

    val savedFolders = mutableListOf<TripDocumentFolder>()
    val updatedFolders = mutableListOf<TripDocumentFolder>()
    val deletedFolders = mutableListOf<TripDocumentFolder>()
    val savedDocuments = mutableListOf<TripDocument>()
    val updatedDocuments = mutableListOf<TripDocument>()
    val deletedDocuments = mutableListOf<TripDocument>()

    override fun getRootFolders(tripId: Int): Flow<List<TripDocumentFolder>> =
        flowOf(rootFolders[tripId] ?: emptyList())

    override fun getSubFolders(parentFolderId: Int): Flow<List<TripDocumentFolder>> =
        flowOf(subFolders[parentFolderId] ?: emptyList())

    override fun getDocumentsInFolder(folderId: Int): Flow<List<TripDocument>> =
        flowOf(documents[folderId] ?: emptyList())

    override fun getRootDocuments(tripId: Int): Flow<List<TripDocument>> =
        flowOf(rootDocuments[tripId] ?: emptyList())

    override fun getDocumentById(id: Int): Flow<TripDocument?> =
        flowOf(
            (documents.values.flatten() + rootDocuments.values.flatten())
                .firstOrNull { it.id == id },
        )

    override fun getAllFoldersForTrip(tripId: Int): Flow<List<TripDocumentFolder>> =
        flowOf(emptyList())

    override suspend fun saveFolder(folder: TripDocumentFolder) {
        savedFolders.add(folder)
    }

    override suspend fun updateFolder(folder: TripDocumentFolder) {
        updatedFolders.add(folder)
    }

    override suspend fun deleteFolder(folder: TripDocumentFolder) {
        deletedFolders.add(folder)
    }

    override suspend fun saveDocument(document: TripDocument) {
        savedDocuments.add(document)
    }

    override suspend fun updateDocument(document: TripDocument) {
        updatedDocuments.add(document)
    }

    override suspend fun deleteDocument(document: TripDocument) {
        deletedDocuments.add(document)
    }

    override suspend fun copyDocumentToInternalStorage(sourceUri: String): String? =
        "file:///fake/documents/copy.pdf"

    override suspend fun getAllDocumentUrisForTrip(tripId: Int): List<String> =
        (rootDocuments[tripId]?.map { it.uri } ?: emptyList()) +
            documents.values.flatten().filter { it.tripId == tripId }.map { it.uri }

    override suspend fun deleteDocumentFileByUri(fileUri: String) {
        // no-op in tests
    }
}

private class FakeDocumentSummaryRepository(
    private val result: DocumentExtractionResult?,
    private val suggestedName: String? = null,
    private val available: Boolean = true,
) : DocumentSummaryRepository {
    var lastTripYear: Int? = null

    override suspend fun isAvailable(): Boolean = available

    override suspend fun extractDocumentInfo(
        fileUri: String,
        mimeType: String,
        tripYear: Int?,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): DocumentExtractionResult? {
        lastTripYear = tripYear
        return result
    }

    override suspend fun suggestDocumentName(
        fileUri: String,
        mimeType: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): String? = suggestedName
}
