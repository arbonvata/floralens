package com.arbon.vata.floralens.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arbon.vata.floralens.data.photos.ScanPhotoStore
import com.arbon.vata.floralens.presentation.screens.AnalyzingScreen
import com.arbon.vata.floralens.presentation.screens.AnalyzingStep
import com.arbon.vata.floralens.presentation.screens.CameraScreen
import com.arbon.vata.floralens.presentation.screens.HistoryScreen
import com.arbon.vata.floralens.presentation.screens.MyPlantsScreen
import com.arbon.vata.floralens.presentation.screens.OnboardingScreen
import com.arbon.vata.floralens.presentation.screens.ResultSupportedScreen
import com.arbon.vata.floralens.presentation.screens.ResultUnsupportedScreen
import com.arbon.vata.floralens.presentation.screens.SupportedPlantsScreen
import com.arbon.vata.floralens.presentation.viewmodel.PlantViewModel
import com.arbon.vata.floralens.presentation.viewmodel.ScanState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Supported : Screen("supported")
    object Camera : Screen("camera")
    object Analyzing : Screen("analyzing")
    object ResultSupported : Screen("result_supported")
    object ResultUnsupported : Screen("result_unsupported")
    object History : Screen("history")
    object MyPlants : Screen("my_plants")
}

/** Bottom navigation tabs per spec: Scan | History | My Plants, with Camera the default. */
private data class TabDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

private val tabs =
    listOf(
        TabDestination(Screen.Camera, "Scan", Icons.Filled.PhotoCamera),
        TabDestination(Screen.History, "History", Icons.Filled.History),
        TabDestination(Screen.MyPlants, "My Plants", Icons.Filled.Eco),
    )

@Composable
fun PlantDoctorNavGraph(
    vm: PlantViewModel = koinViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onTab = tabs.any { tab -> currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true }

    Scaffold(
        topBar = {
            if (onTab) {
                FloraLensTopBar(navController)
            }
        },
        bottomBar = {
            if (onTab) {
                FloraLensBottomBar(navController, currentDestination)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (vm.isOnboardingDone) Screen.Camera.route else Screen.Onboarding.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onSeePlantsClick = {
                        vm.completeOnboarding()
                        navController.navigate(Screen.Supported.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onStartScanClick = {
                        vm.completeOnboarding()
                        navController.navigate(Screen.Camera.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Screen.Supported.route) {
                SupportedPlantsScreen(
                    onGotItClick = { navController.navigate(Screen.Camera.route) },
                    onNotifyBanana = { /* vm.save wishlist record */ },
                    onNotifyAvocado = { }
                )
            }

            composable(Screen.Camera.route) {
                CameraScreen(
                    onShutterClick = {
                        navController.navigate(Screen.Analyzing.route)
                        // In real app: vm.scan(bitmap, path) called from CameraX callback
                    },
                    onGalleryClick = { /* open gallery -> vm.scan() */ }
                )
            }

            composable(Screen.Analyzing.route) {
                val state = ScanState.Scanning  // just to compile - replace with vm.state

                // Observe vm.state
                when(val s = state) {
                    is ScanState.Scanning -> AnalyzingScreen(step = AnalyzingStep.ON_DEVICE)
                    is ScanState.OnDeviceResult -> {
                        if(s.needsCloud) {
                            AnalyzingScreen(step = AnalyzingStep.SECOND_OPINION)
                            // After cloud call, vm will emit CloudResult
                        } else {
                            navController.navigate(Screen.ResultSupported.route) {
                                popUpTo(Screen.Camera.route)
                            }
                        }
                    }
                    is ScanState.CloudResult -> {
                        navController.navigate(Screen.ResultSupported.route) {
                            popUpTo(Screen.Camera.route)
                        }
                    }
                    is ScanState.Unsupported -> {
                        navController.navigate(Screen.ResultUnsupported.route) {
                            popUpTo(Screen.Camera.route)
                        }
                    }
                    else -> AnalyzingScreen()
                }
            }

            composable(Screen.ResultSupported.route) {
                val state = ScanState.Scanning  // just to compile - replace with vm.state
                val current = null
                if(current != null) {
                    ResultSupportedScreen(
                        record = current,
                        needsCloud = (state as? ScanState.CloudResult) != null,
                        onSaveClick = { navController.navigate(Screen.History.route) },
                        onTrackClick = { navController.navigate(Screen.MyPlants.route) },
                        onCorrectClick = { /* open correction dialog -> update ObjectBox */ }
                    )
                }
            }

            composable(Screen.ResultUnsupported.route) {
                val state = ScanState.Scanning  // just to compile - replace with vm.state
                val species = (state as? ScanState.Unsupported)?.species ?: "Banana"
                ResultUnsupportedScreen(
                    species = species,
                    onNotifyClick = {
                        // vm.save wishlist record (wantsNotifyV2 = true)
                        navController.navigate(Screen.History.route)
                    },
                    onCareTipsClick = { /* call Trefle API */ },
                    onSecondOpinionClick = {
                        navController.navigate(Screen.Analyzing.route)
                        // vm.scanWithCloud()
                    }
                )
            }

            composable(Screen.History.route) {
                val records by vm.history.collectAsState()
                val photoStore: ScanPhotoStore = koinInject()
                HistoryScreen(
                    records = records,
                    resolvePhoto = { record -> record.photoPath?.let(photoStore::file) },
                    onItemClick = { record ->
                        if (record.isSupported) navController.navigate(Screen.ResultSupported.route)
                        else navController.navigate(Screen.ResultUnsupported.route)
                    },
                )
            }

            composable(Screen.MyPlants.route) {
                MyPlantsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloraLensTopBar(
    navController: NavHostController,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Flora Lens") },
        actions = {
            IconButton(onClick = { /* search: wired when History search lands */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Supported plants") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate(Screen.Supported.route)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    enabled = false,
                    onClick = { },
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    enabled = false,
                    onClick = { },
                )
            }
        },
    )
}

@Composable
private fun FloraLensBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected =
                currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}