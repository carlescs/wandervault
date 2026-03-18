package cat.company.wandervault.data.remote.google

import android.content.Intent

/**
 * Android-specific component that manages the interactive Google Sign-In flow.
 *
 * This interface lives in the data layer (not the domain) because it exposes
 * Android platform types ([Intent]) that the domain layer must not reference.
 * It is satisfied by [GoogleDriveRepositoryImpl] and injected directly into the
 * presentation layer for the sign-in activity handshake.
 */
interface DriveSignInClient {

    /** Returns the [Intent] that launches the Google Sign-In chooser activity. */
    fun buildSignInIntent(): Intent

    /**
     * Processes the [Intent] returned from the Google Sign-In activity.
     *
     * @param data The result intent from the sign-in activity, or `null` if the user
     *   cancelled.
     * @return [Result.success] on successful sign-in, [Result.failure] otherwise.
     */
    suspend fun handleSignInResult(data: Intent?): Result<Unit>
}
