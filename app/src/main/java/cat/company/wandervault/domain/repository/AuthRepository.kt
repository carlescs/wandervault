package cat.company.wandervault.domain.repository

import android.content.Intent
import cat.company.wandervault.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Firebase Authentication.
 *
 * The sign-in flow is intent-based (Google Sign-In):
 * 1. Call [buildSignInIntent] to get an [Intent] to launch via `ActivityResultLauncher`.
 * 2. Pass the result [Intent] to [handleSignInResult] to complete sign-in.
 */
interface AuthRepository {
    /** Emits the currently signed-in [User], or `null` if no user is signed in. */
    fun currentUser(): Flow<User?>

    /** Returns `true` if a user is currently signed in. */
    suspend fun isSignedIn(): Boolean

    /** Builds the Google Sign-In [Intent] to be launched by the UI layer. */
    fun buildSignInIntent(): Intent

    /**
     * Exchanges the [Intent] returned from the Google Sign-In activity for a Firebase credential
     * and signs in. Returns [Result.success] with the signed-in [User] on success, or
     * [Result.failure] on error.
     */
    suspend fun handleSignInResult(data: Intent?): Result<User>

    /** Signs the current user out of Firebase. */
    suspend fun signOut()
}
