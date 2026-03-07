package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_documents",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["folderId", "name"], unique = true),
    ],
    foreignKeys = [
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
    /** The folder this document belongs to. */
    val folderId: Int,
    val name: String,
    /** URI pointing to the document in internal storage (same pattern as trip cover images). */
    val uri: String,
    /** MIME type or simple type tag, e.g. "pdf", "image", "note". */
    val mimeType: String,
)
