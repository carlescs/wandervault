package cat.company.wandervault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cat.company.wandervault.data.local.TripDocumentDao
import cat.company.wandervault.data.local.TripDocumentEntity
import cat.company.wandervault.data.local.TripDocumentFolderDao
import cat.company.wandervault.data.local.TripDocumentFolderEntity
import cat.company.wandervault.data.local.TripEntity
import cat.company.wandervault.data.local.WanderVaultDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [TripDocumentFolderDao] and [TripDocumentDao].
 *
 * Uses an in-memory Room database so no persistent storage is affected.
 */
@RunWith(AndroidJUnit4::class)
class TripDocumentDaoTest {

    private lateinit var db: WanderVaultDatabase
    private lateinit var folderDao: TripDocumentFolderDao
    private lateinit var documentDao: TripDocumentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WanderVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        folderDao = db.tripDocumentFolderDao()
        documentDao = db.tripDocumentDao()

        // Insert a parent trip so FK constraints are satisfied.
        runTest {
            db.tripDao().insert(TripEntity(id = 1, title = "Test Trip"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── TripDocumentFolderDao ─────────────────────────────────────────────────

    @Test
    fun insertAndGetRootFolders() = runTest {
        folderDao.insert(TripDocumentFolderEntity(tripId = 1, name = "Hotels"))
        folderDao.insert(TripDocumentFolderEntity(tripId = 1, name = "Flights"))

        val folders = folderDao.getRootFolders(tripId = 1).first()

        assertEquals(2, folders.size)
        // Root folders are ordered by name ascending.
        assertEquals("Flights", folders[0].name)
        assertEquals("Hotels", folders[1].name)
    }

    @Test
    fun insertAndGetSubFolders() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 10, tripId = 1, name = "Parent"))
        folderDao.insert(TripDocumentFolderEntity(tripId = 1, name = "Child", parentFolderId = 10))

        val subFolders = folderDao.getSubFolders(parentFolderId = 10).first()

        assertEquals(1, subFolders.size)
        assertEquals("Child", subFolders[0].name)
        assertEquals(10, subFolders[0].parentFolderId)
    }

    @Test
    fun updateFolder() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 5, tripId = 1, name = "OldName"))

        folderDao.update(TripDocumentFolderEntity(id = 5, tripId = 1, name = "NewName"))

        val folders = folderDao.getRootFolders(tripId = 1).first()
        assertEquals(1, folders.size)
        assertEquals("NewName", folders[0].name)
    }

    @Test
    fun deleteFolder() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 7, tripId = 1, name = "ToDelete"))

        folderDao.delete(TripDocumentFolderEntity(id = 7, tripId = 1, name = "ToDelete"))

        val folders = folderDao.getRootFolders(tripId = 1).first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun deleteFolder_cascadesToSubFolders() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 20, tripId = 1, name = "Parent"))
        folderDao.insert(TripDocumentFolderEntity(id = 21, tripId = 1, name = "Child", parentFolderId = 20))

        folderDao.delete(TripDocumentFolderEntity(id = 20, tripId = 1, name = "Parent"))

        val subFolders = folderDao.getSubFolders(parentFolderId = 20).first()
        assertTrue(subFolders.isEmpty())
    }

    // ── TripDocumentDao ───────────────────────────────────────────────────────

    @Test
    fun insertAndGetDocumentsByFolder() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 100, tripId = 1, name = "Docs"))

        documentDao.insert(TripDocumentEntity(folderId = 100, name = "doc_b.pdf", uri = "uri_b", mimeType = "application/pdf"))
        documentDao.insert(TripDocumentEntity(folderId = 100, name = "doc_a.pdf", uri = "uri_a", mimeType = "application/pdf"))

        val docs = documentDao.getByFolderId(folderId = 100).first()

        assertEquals(2, docs.size)
        // Documents are ordered by name ascending.
        assertEquals("doc_a.pdf", docs[0].name)
        assertEquals("doc_b.pdf", docs[1].name)
    }

    @Test
    fun updateDocument() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 200, tripId = 1, name = "Folder"))
        documentDao.insert(TripDocumentEntity(id = 300, folderId = 200, name = "original.pdf", uri = "uri", mimeType = "application/pdf"))

        documentDao.update(TripDocumentEntity(id = 300, folderId = 200, name = "renamed.pdf", uri = "uri", mimeType = "application/pdf"))

        val docs = documentDao.getByFolderId(folderId = 200).first()
        assertEquals(1, docs.size)
        assertEquals("renamed.pdf", docs[0].name)
    }

    @Test
    fun deleteDocument() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 400, tripId = 1, name = "Folder"))
        val entity = TripDocumentEntity(id = 500, folderId = 400, name = "to_delete.pdf", uri = "uri", mimeType = "application/pdf")
        documentDao.insert(entity)

        documentDao.delete(entity)

        val docs = documentDao.getByFolderId(folderId = 400).first()
        assertTrue(docs.isEmpty())
    }

    @Test
    fun deleteFolder_cascadesToDocuments() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 600, tripId = 1, name = "FolderWithDocs"))
        documentDao.insert(TripDocumentEntity(folderId = 600, name = "doc.pdf", uri = "uri", mimeType = "application/pdf"))

        folderDao.delete(TripDocumentFolderEntity(id = 600, tripId = 1, name = "FolderWithDocs"))

        val docs = documentDao.getByFolderId(folderId = 600).first()
        assertTrue(docs.isEmpty())
    }

    @Test
    fun countRootFoldersByName_returnsCorrectCount() = runTest {
        folderDao.insert(TripDocumentFolderEntity(id = 700, tripId = 1, name = "Unique"))

        val count = folderDao.countRootFoldersByName(tripId = 1, name = "Unique")
        val countMissing = folderDao.countRootFoldersByName(tripId = 1, name = "NotPresent")
        val countExcluded = folderDao.countRootFoldersByName(tripId = 1, name = "Unique", excludeId = 700)

        assertEquals(1, count)
        assertEquals(0, countMissing)
        assertEquals(0, countExcluded)
    }
}
