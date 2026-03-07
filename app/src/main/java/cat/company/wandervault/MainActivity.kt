package cat.company.wandervault

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cat.company.wandervault.ui.LocalSharedTransitionScope
import cat.company.wandervault.ui.navigation.AppRoutes
import cat.company.wandervault.ui.navigation.WanderVaultNavHost
import cat.company.wandervault.ui.screens.ShareScreen
import cat.company.wandervault.ui.theme.WanderVaultTheme

class MainActivity : ComponentActivity() {

    private val shareIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.action == Intent.ACTION_SEND) {
            shareIntentState.value = intent
        }
        setContent {
            WanderVaultTheme {
                WanderVaultApp(shareIntentState = shareIntentState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEND) {
            shareIntentState.value = intent
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@PreviewScreenSizes
@Composable
fun WanderVaultApp(shareIntentState: MutableState<Intent?> = mutableStateOf(null)) {
    val shareIntent by shareIntentState

    if (shareIntent != null) {
        ShareScreen(
            shareIntent = shareIntent!!,
            onNavigateUp = { shareIntentState.value = null },
            onSaved = { shareIntentState.value = null },
        )
        return
    }

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: AppRoutes.HOME

    val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()
    val isTopLevel = currentRoute in topLevelRoutes

    val layoutType = if (isTopLevel) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    } else {
        NavigationSuiteType.None
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavigationSuiteScaffold(
                layoutType = layoutType,
                navigationSuiteItems = {
                    TopLevelDestination.entries.forEach { dest ->
                        item(
                            icon = {
                                Icon(
                                    dest.icon,
                                    contentDescription = stringResource(dest.contentDescriptionRes),
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WanderVaultNavHost(
                        navController = navController,
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding),
                    )
                }
            }
        }
    }
}

/** Top-level navigation destinations shown in the [NavigationSuiteScaffold]. */
enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val contentDescriptionRes: Int,
    val icon: ImageVector,
) {
    HOME(AppRoutes.HOME, R.string.nav_home, R.string.nav_home_desc, Icons.Default.Home),
    FAVORITES(AppRoutes.FAVORITES, R.string.nav_favorites, R.string.nav_favorites_desc, Icons.Default.Favorite),
    PROFILE(AppRoutes.PROFILE, R.string.nav_profile, R.string.nav_profile_desc, Icons.Default.AccountBox),
}

