package cat.company.wandervault.data.remote.firebase

import cat.company.wandervault.data.local.TripDao
import cat.company.wandervault.data.local.TripEntity
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import cat.company.wandervault.domain.repository.TripSyncRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID

/**
 * Firestore implementation of [TripSyncRepository].
 *
 * **Firestore schema** (offline-first, last-write-wins):
 * ```
 * /trips/{shareId}
 *     title            : String
 *     ownerId          : String
 *     collaboratorIds  : List<String>
 *     lastModifiedAt   : Timestamp
 *     imageUri         : String?
 *     aiDescription    : String?
 *     defaultTimezone  : String?
 * /invitations/{inviteCode}
 *     shareId          : String
 *     createdBy        : String   (Firebase UID)
 *     expiresAt        : Timestamp
 * ```
 *
 * Security rules (to be deployed separately) restrict read/write to `ownerId` and
 * members of `collaboratorIds`.
 */
class FirestoreTripSyncRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val tripRepository: TripRepository,
    private val tripDao: TripDao,
) : TripSyncRepository {

    private val tripsCollection get() = firestore.collection("trips")
    private val invitesCollection get() = firestore.collection("invitations")

    // ── Sharing ──────────────────────────────────────────────────────────────

    override suspend fun shareTrip(tripId: Int): String {
        val uid = requireSignedIn()
        val shareId = UUID.randomUUID().toString()

        pushTripToFirestore(tripId, shareId, uid, emptyList())

        tripRepository.updateTripShareInfo(
            tripId = tripId,
            shareId = shareId,
            ownerId = uid,
            collaboratorIds = emptyList(),
        )
        return shareId
    }

    override suspend fun unshareTrip(tripId: Int) {
        val uid = requireSignedIn()
        val entity = tripDao.getByIdOnce(tripId) ?: return
        val shareId = entity.shareId ?: return
        require(entity.ownerId == uid) { "Only the trip owner can unshare a trip." }

        tripsCollection.document(shareId).delete().await()

        tripRepository.updateTripShareInfo(
            tripId = tripId,
            shareId = null,
            ownerId = null,
            collaboratorIds = emptyList(),
        )
    }

    override suspend fun joinTrip(shareId: String): Trip {
        val uid = requireSignedIn()

        val doc = tripsCollection.document(shareId).get().await()
        check(doc.exists()) { "No shared trip found for shareId=$shareId" }

        val title = doc.getString("title") ?: "Shared Trip"
        val ownerId = doc.getString("ownerId") ?: ""
        @Suppress("UNCHECKED_CAST")
        val collaboratorIds = (doc.get("collaboratorIds") as? List<String>).orEmpty()

        // Add current user as collaborator if not already present.
        if (uid !in collaboratorIds && uid != ownerId) {
            tripsCollection.document(shareId)
                .update("collaboratorIds", FieldValue.arrayUnion(uid))
                .await()
        }

        val updatedCollaborators = (collaboratorIds + uid).distinct().filter { it != ownerId }
        val trip = Trip(
            id = 0,
            title = title,
            shareId = shareId,
            ownerId = ownerId,
            collaboratorIds = updatedCollaborators,
        )
        tripRepository.saveTrip(trip)
        return trip
    }

    override suspend fun leaveTrip(shareId: String) {
        val uid = requireSignedIn()

        tripsCollection.document(shareId)
            .update("collaboratorIds", FieldValue.arrayRemove(uid))
            .await()

        val entity = tripDao.getByShareId(shareId) ?: return
        tripRepository.deleteTrip(
            Trip(
                id = entity.id,
                title = entity.title,
                shareId = entity.shareId,
                ownerId = entity.ownerId,
                collaboratorIds = entity.collaboratorIds,
            ),
        )
    }

    override fun observeSharedTrip(shareId: String): Flow<Trip?> =
        tripRepository.getTrips().map { trips -> trips.firstOrNull { it.shareId == shareId } }

    override suspend fun pushLocalChanges(tripId: Int) {
        val uid = requireSignedIn()
        val entity = tripDao.getByIdOnce(tripId) ?: return
        val shareId = entity.shareId ?: error("Trip $tripId has not been shared yet.")
        pushTripToFirestore(tripId, shareId, uid, entity.collaboratorIds)
    }

    // ── Invitations ──────────────────────────────────────────────────────────

    override suspend fun createInviteCode(shareId: String): String {
        val uid = requireSignedIn()
        val code = generateInviteCode()
        val expiresAt = Instant.now().plusSeconds(7 * 24 * 3600L) // 7 days

        invitesCollection.document(code).set(
            mapOf(
                "shareId" to shareId,
                "createdBy" to uid,
                "expiresAt" to Timestamp(expiresAt.epochSecond, 0),
            ),
        ).await()
        return code
    }

    override suspend fun acceptInviteCode(inviteCode: String): String {
        val doc = invitesCollection.document(inviteCode).get().await()
        require(doc.exists()) { "Invalid or expired invite code." }

        val expiresAt = doc.getTimestamp("expiresAt")
        require(
            expiresAt == null || expiresAt.seconds > Instant.now().epochSecond,
        ) { "Invite code has expired." }

        return doc.getString("shareId") ?: error("Invite document is missing shareId.")
    }

    override suspend fun removeCollaborator(shareId: String, collaboratorUid: String) {
        requireSignedIn()
        tripsCollection.document(shareId)
            .update("collaboratorIds", FieldValue.arrayRemove(collaboratorUid))
            .await()

        val entity = tripDao.getByShareId(shareId) ?: return
        tripRepository.updateTripShareInfo(
            tripId = entity.id,
            shareId = shareId,
            ownerId = entity.ownerId,
            collaboratorIds = entity.collaboratorIds.filter { it != collaboratorUid },
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun requireSignedIn(): String =
        auth.currentUser?.uid ?: error("User must be signed in to perform this operation.")

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private suspend fun pushTripToFirestore(
        tripId: Int,
        shareId: String,
        ownerId: String,
        collaboratorIds: List<String>,
    ) {
        val entity = tripDao.getByIdOnce(tripId) ?: return

        val tripDoc = mapOf(
            "title" to entity.title,
            "ownerId" to ownerId,
            "collaboratorIds" to collaboratorIds,
            "lastModifiedAt" to FieldValue.serverTimestamp(),
            "imageUri" to entity.imageUri,
            "aiDescription" to entity.aiDescription,
            "defaultTimezone" to entity.defaultTimezone,
        )
        tripsCollection.document(shareId).set(tripDoc, SetOptions.merge()).await()
    }
}

private fun TripEntity.toDomain() = Trip(
    id = id,
    title = title,
    imageUri = imageUri,
    aiDescription = aiDescription,
    isFavorite = isFavorite,
    defaultTimezone = defaultTimezone,
    nextStep = nextStep,
    nextStepDeadline = nextStepDeadline,
    isArchived = isArchived,
    shareId = shareId,
    ownerId = ownerId,
    collaboratorIds = collaboratorIds,
)
