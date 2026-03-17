package cat.company.wandervault.ui.navigation

/** Route patterns and route-builder helpers for the WanderVault [NavHost][androidx.navigation.compose.NavHost]. */
internal object AppRoutes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val LOCATION_DETAIL = "location_detail/{destinationId}"
    const val TRANSPORT_DETAIL = "transport_detail/{destinationId}"
    const val DOCUMENT_INFO = "document_info/{documentId}"
    const val DOCUMENT_CHAT = "document_chat/{documentId}"
    const val SETTINGS = "settings"
    const val DATA_ADMIN = "data_admin"

    /** Returns the navigable route for a specific trip detail screen. */
    fun tripDetail(tripId: Int) = "trip_detail/$tripId"

    /** Returns the navigable route for a specific location detail screen. */
    fun locationDetail(destinationId: Int) = "location_detail/$destinationId"

    /** Returns the navigable route for a specific transport detail screen. */
    fun transportDetail(destinationId: Int) = "transport_detail/$destinationId"

    /** Returns the navigable route for a specific document info screen. */
    fun documentInfo(documentId: Int) = "document_info/$documentId"

    /** Returns the navigable route for the document chat screen for a specific document. */
    fun documentChat(documentId: Int) = "document_chat/$documentId"
}
