package cat.company.wandervault.data.remote.firebase

import android.content.Context
import android.content.Intent
import cat.company.wandervault.domain.model.User
import cat.company.wandervault.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of [AuthRepository].
 *
 * Uses Google Sign-In to obtain a credential that is then exchanged for a Firebase Auth token.
 * The web client ID must match the one configured in the Firebase console.
 */
class FirebaseAuthRepositoryImpl(
    private val context: Context,
    private val auth: FirebaseAuth,
    /** Web client ID from the Firebase console (OAuth 2.0 web client ID). */
    private val webClientId: String,
) : AuthRepository {

    override fun currentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun isSignedIn(): Boolean = auth.currentUser != null

    override fun buildSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    override suspend fun handleSignInResult(data: Intent?): Result<User> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user?.toDomain()
                ?: return Result.failure(IllegalStateException("Sign-in succeeded but user is null"))
            Result.success(user)
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        // Also sign out from Google so the account chooser appears on next sign-in.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut().await()
    }
}

private fun com.google.firebase.auth.FirebaseUser.toDomain() = User(
    uid = uid,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl?.toString(),
)
