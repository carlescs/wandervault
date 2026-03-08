package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_documents",
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["folderId"]),
        /**
         * Unique index on `[folderId, name]` prevents duplicate document names within the same
         * folder. Note: SQLite treats NULL values as distinct in unique indexes, so this constraint
         * does not prevent two root-level documents (folderId IS NULL) from having the same name in
         * the same trip. Uniqueness for root-level documents is enforced in the repository layer.
         */
        Index(value = ["folderId", "name"], unique = true),
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
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TripDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** The trip this document belongs to. */
    val tripId: Int,
    /** The folder this document belongs to, or `null` for root-level documents. */
    val folderId: Int? = null,
    val name: String,
    /** URI pointing to the document in internal storage (same pattern as trip cover images). */
    val uri: String,
    /** MIME type or simple type tag, e.g. "pdf", "image", "note". */
    val mimeType: String,
)
