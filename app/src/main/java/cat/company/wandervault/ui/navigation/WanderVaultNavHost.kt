package cat.company.wandervault.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cat.company.wandervault.ui.LocalAnimatedVisibilityScope
import cat.company.wandervault.ui.screens.ArchiveScreen
import cat.company.wandervault.ui.screens.DataAdminScreen
import cat.company.wandervault.ui.screens.DocumentChatScreen
import cat.company.wandervault.ui.screens.DocumentInfoScreen
import cat.company.wandervault.ui.screens.FavoritesScreen
import cat.company.wandervault.ui.screens.HomeScreen
import cat.company.wandervault.ui.screens.LocationDetailScreen
import cat.company.wandervault.ui.screens.ProfileScreen
import cat.company.wandervault.ui.screens.SettingsScreen
import cat.company.wandervault.ui.screens.TransportDetailScreen
import cat.company.wandervault.ui.screens.TripDetailScreen

/**
 * Defines the full navigation graph for WanderVault via [NavHost].
 *
 * Every named route is declared here; screens receive navigation callbacks instead of direct
 * state-mutation lambdas, keeping them decoupled from the navigation host.
 *
 * Shared-element transitions between [HomeScreen] and [TripDetailScreen] are enabled by
 * providing [LocalAnimatedVisibilityScope] for those two destinations. The surrounding
 * [SharedTransitionLayout][androidx.compose.animation.SharedTransitionLayout] and
 * [LocalSharedTransitionScope][cat.company.wandervault.ui.LocalSharedTransitionScope] are
 * set up by the caller ([cat.company.wandervault.WanderVaultApp]).
 *
 * @param navController The [NavHostController] that drives navigation.
 * @param modifier Optional [Modifier] applied to the [NavHost] container.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun WanderVaultNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
        modifier = modifier,
    ) {
        composable(AppRoutes.HOME) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                HomeScreen(
                    onTripClick = { tripId -> navController.navigate(AppRoutes.tripDetail(tripId)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composable(AppRoutes.FAVORITES) {
            FavoritesScreen(
                onTripClick = { tripId -> navController.navigate(AppRoutes.tripDetail(tripId)) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(AppRoutes.ARCHIVE) {
            ArchiveScreen(
                onTripClick = { tripId -> navController.navigate(AppRoutes.tripDetail(tripId)) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(AppRoutes.PROFILE) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = AppRoutes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getInt("tripId")?.takeIf { it > 0 } ?: run {
                navController.navigateUp()
                return@composable
            }
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                TripDetailScreen(
                    tripId = tripId,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToDestination = { destinationId ->
                        navController.navigate(AppRoutes.locationDetail(destinationId))
                    },
                    onNavigateToTransport = { destinationId ->
                        navController.navigate(AppRoutes.transportDetail(destinationId))
                    },
                    onNavigateToDocument = { documentId ->
                        navController.navigate(AppRoutes.documentInfo(documentId))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composable(
            route = AppRoutes.LOCATION_DETAIL,
            arguments = listOf(navArgument("destinationId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getInt("destinationId")?.takeIf { it > 0 } ?: run {
                navController.navigateUp()
                return@composable
            }
            LocationDetailScreen(
                destinationId = destinationId,
                onNavigateUp = { navController.navigateUp() },
                onTransportClick = { navController.navigate(AppRoutes.transportDetail(it)) },
                onNavigateToDocument = { navController.navigate(AppRoutes.documentInfo(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = AppRoutes.TRANSPORT_DETAIL,
            arguments = listOf(navArgument("destinationId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getInt("destinationId")?.takeIf { it > 0 } ?: run {
                navController.navigateUp()
                return@composable
            }
            TransportDetailScreen(
                destinationId = destinationId,
                onNavigateUp = { navController.navigateUp() },
                onNavigateToDocument = { navController.navigate(AppRoutes.documentInfo(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToDataAdmin = { navController.navigate(AppRoutes.DATA_ADMIN) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(AppRoutes.DATA_ADMIN) {
            DataAdminScreen(
                onNavigateUp = { navController.navigateUp() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = AppRoutes.DOCUMENT_INFO,
            arguments = listOf(navArgument("documentId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getInt("documentId")?.takeIf { it > 0 } ?: run {
                navController.navigateUp()
                return@composable
            }
            DocumentInfoScreen(
                documentId = documentId,
                onNavigateUp = { navController.navigateUp() },
                onNavigateToChat = { navController.navigate(AppRoutes.documentChat(documentId)) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = AppRoutes.DOCUMENT_CHAT,
            arguments = listOf(navArgument("documentId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getInt("documentId")?.takeIf { it > 0 } ?: run {
                navController.navigateUp()
                return@composable
            }
            DocumentChatScreen(
                documentId = documentId,
                onNavigateUp = { navController.navigateUp() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
