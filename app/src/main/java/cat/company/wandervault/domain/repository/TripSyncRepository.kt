package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Trip
import kotlinx.coroutines.flow.Flow

/**
 * Repository that handles syncing a local [Trip] to/from Cloud Firestore for real-time
 * collaboration.
 *
 * Design decisions:
 * - **Offline-first**: Room is the source of truth.  Firestore is updated best-effort.
 * - **Last-write-wins**: each document carries a `lastModifiedAt` timestamp; the most recent
 *   write wins on conflict.
 * - **Per-trip granularity**: the entire trip (destinations, transport, hotels) is synced
 *   together when [pushLocalChanges] is called.
 */
interface TripSyncRepository {
    /**
     * Uploads [tripId] to Firestore, making it shareable.  Sets `Trip.shareId` and `Trip.ownerId`
     * in the local Room database.
     *
     * @return The newly generated share ID (Firestore document ID).
     * @throws IllegalStateException if no user is signed in.
     */
    suspend fun shareTrip(tripId: Int): String

    /**
     * Removes the shared trip from Firestore and clears `shareId` / `ownerId` / `collaboratorIds`
     * locally.  Only the owner may call this.
     */
    suspend fun unshareTrip(tripId: Int)

    /**
     * Joins a shared trip identified by [shareId].  Downloads the trip from Firestore and saves
     * it locally in Room.
     *
     * @return The local [Trip] that was created.
     * @throws IllegalStateException if no user is signed in or the invite is invalid.
     */
    suspend fun joinTrip(shareId: String): Trip

    /**
     * Removes the current user from the collaborators list of [shareId] and deletes the local
     * copy of the trip.
     */
    suspend fun leaveTrip(shareId: String)

    /**
     * Returns a [Flow] that emits an updated [Trip] every time the Firestore document for
     * [shareId] changes.
     */
    fun observeSharedTrip(shareId: String): Flow<Trip?>

    /**
     * Pushes the current local state of [tripId] to Firestore, overwriting the remote copy.
     * Only the owner or a collaborator may call this.
     */
    suspend fun pushLocalChanges(tripId: Int)

    /**
     * Creates a short invite code in Firestore for the shared trip.
     *
     * @param shareId The Firestore document ID of the shared trip.
     * @return A 6-character alphanumeric invite code.
     */
    suspend fun createInviteCode(shareId: String): String

    /**
     * Validates [inviteCode], adds the current user to the trip's `collaboratorIds`, and returns
     * the resolved share ID so [joinTrip] can be called.
     *
     * @return The `shareId` that corresponds to the invite code.
     * @throws IllegalArgumentException if the code is invalid or expired.
     */
    suspend fun acceptInviteCode(inviteCode: String): String

    /**
     * Removes [collaboratorUid] from the shared trip's `collaboratorIds` list in Firestore and
     * locally.  Only the owner may call this.
     */
    suspend fun removeCollaborator(shareId: String, collaboratorUid: String)
}
