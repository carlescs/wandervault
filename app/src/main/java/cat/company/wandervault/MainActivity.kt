package cat.company.wandervault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import cat.company.wandervault.ui.LocalAnimatedVisibilityScope
import cat.company.wandervault.ui.LocalSharedTransitionScope
import cat.company.wandervault.ui.screens.DataAdminScreen
import cat.company.wandervault.ui.screens.FavoritesScreen
import cat.company.wandervault.ui.screens.HomeScreen
import cat.company.wandervault.ui.screens.LocationDetailScreen
import cat.company.wandervault.ui.screens.ProfileScreen
import cat.company.wandervault.ui.screens.SettingsScreen
import cat.company.wandervault.ui.screens.TransportDetailScreen
import cat.company.wandervault.ui.screens.TripDetailScreen
import cat.company.wandervault.ui.theme.WanderVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WanderVaultTheme {
                WanderVaultApp()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@PreviewScreenSizes
@Composable
fun WanderVaultApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var tripDetailId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedDestinationId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedTransportDestinationId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showDataAdmin by rememberSaveable { mutableStateOf(false) }
    val saveableStateHolder = rememberSaveableStateHolder()

    if (showDataAdmin) {
        BackHandler { showDataAdmin = false }
        DataAdminScreen(
            onNavigateUp = { showDataAdmin = false },
            modifier = Modifier.fillMaxSize(),
        )
    } else if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            onNavigateUp = { showSettings = false },
            onNavigateToDataAdmin = { showDataAdmin = true },
            modifier = Modifier.fillMaxSize(),
        )
    } else if (selectedTransportDestinationId != null) {
        BackHandler { selectedTransportDestinationId = null }
        selectedTransportDestinationId?.let { destinationId ->
            TransportDetailScreen(
                destinationId = destinationId,
                onNavigateUp = { selectedTransportDestinationId = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else if (selectedDestinationId != null) {
        BackHandler { selectedDestinationId = null }
        selectedDestinationId?.let { destinationId ->
            LocationDetailScreen(
                destinationId = destinationId,
                onNavigateUp = { selectedDestinationId = null },
                onTransportClick = { selectedTransportDestinationId = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = tripDetailId,
                label = "trip-detail-transition",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300, delayMillis = 100)) togetherWith
                        fadeOut(animationSpec = tween(150))
                },
            ) { currentTripId ->
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    if (currentTripId != null) {
                        BackHandler {
                            saveableStateHolder.removeState("TripDetail:$currentTripId")
                            tripDetailId = null
                        }
                        val tripDetailKey = "TripDetail:$currentTripId"
                        saveableStateHolder.SaveableStateProvider(key = tripDetailKey) {
                            TripDetailScreen(
                                tripId = currentTripId,
                                onNavigateUp = {
                                    saveableStateHolder.removeState(tripDetailKey)
                                    tripDetailId = null
                                },
                                onNavigateToDestination = { selectedDestinationId = it },
                                onNavigateToTransport = { selectedTransportDestinationId = it },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
                        NavigationSuiteScaffold(
                            navigationSuiteItems = {
                                AppDestinations.entries.forEach {
                                    item(
                                        icon = {
                                            Icon(
                                                it.icon,
                                                contentDescription = stringResource(it.contentDescriptionRes)
                                            )
                                        },
                                        label = { Text(stringResource(it.labelRes)) },
                                        selected = it == currentDestination,
                                        onClick = { currentDestination = it }
                                    )
                                }
                            }
                        ) {
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                when (currentDestination) {
                                    AppDestinations.HOME -> HomeScreen(
                                        modifier = Modifier.padding(innerPadding),
                                        onTripClick = { tripDetailId = it },
                                    )
                                    AppDestinations.FAVORITES -> FavoritesScreen(modifier = Modifier.padding(innerPadding))
                                    AppDestinations.PROFILE -> ProfileScreen(
                                        onNavigateToSettings = { showSettings = true },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val labelRes: Int,
    val contentDescriptionRes: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, R.string.nav_home_desc, Icons.Default.Home),
    FAVORITES(R.string.nav_favorites, R.string.nav_favorites_desc, Icons.Default.Favorite),
    PROFILE(R.string.nav_profile, R.string.nav_profile_desc, Icons.Default.AccountBox),
}