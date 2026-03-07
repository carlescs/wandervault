package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_document_folders",
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["parentFolderId"]),
    /**
     * Unique index on `[tripId, parentFolderId, name]` prevents duplicate folder names within the
     * same parent scope. Note: SQLite treats NULL values as distinct in unique indexes, so this
     * constraint does not prevent two root-level folders (parentFolderId IS NULL) from having the
     * same name in the same trip. Uniqueness for root-level folders is enforced in the repository
     * layer via [cat.company.wandervault.data.repository.TripDocumentRepositoryImpl].
     */
    Index(value = ["tripId", "parentFolderId", "name"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TripDocumentFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TripDocumentFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val name: String,
    /** `null` for root-level folders; set to the parent's ID for sub-folders. */
    val parentFolderId: Int? = null,
)
