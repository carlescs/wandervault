package cat.company.wandervault.domain.model

/**
 * Represents a signed-in user.
 *
 * @param uid Unique Firebase user ID.
 * @param displayName Display name of the user, or `null` if not set.
 * @param email Email address of the user, or `null` if not available.
 * @param photoUrl Profile photo URL, or `null` if not set.
 */
data class User(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)
