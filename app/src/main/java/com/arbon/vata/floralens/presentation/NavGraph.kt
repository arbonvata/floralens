package com.arbon.vata.floralens.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
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
    object ResultSupported : Screen("result_supported?recordId={recordId}") {
        fun scanRoute() = "result_supported"

        fun historyRoute(recordId: Long) = "result_supported?recordId=$recordId"
    }
    object ResultUnsupported : Screen("result_unsupported?recordId={recordId}") {
        fun scanRoute() = "result_unsupported"

        fun historyRoute(recordId: Long) = "result_unsupported?recordId=$recordId"
    }
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
                val requestedWishlistSpecies by vm.requestedWishlistSpecies.collectAsState()
                SupportedPlantsScreen(
                    onGotItClick = { navController.navigate(Screen.Camera.route) },
                    onNotifyBanana = { vm.toggleWishlistRequest("Banana") },
                    onNotifyAvocado = { vm.toggleWishlistRequest("Avocado") },
                    bananaRequested = "banana" in requestedWishlistSpecies,
                    avocadoRequested = "avocado" in requestedWishlistSpecies,
                )
            }

            composable(Screen.Camera.route) {
                CameraScreen(
                    onPhotoCaptured = { bitmap, focusRegion ->
                        vm.scan(bitmap, focusRegion)
                        navController.navigate(Screen.Analyzing.route)
                    },
                    onPhotoPicked = { uri ->
                        vm.scan(uri)
                        navController.navigate(Screen.Analyzing.route)
                    },
                )
            }

            composable(Screen.Analyzing.route) {
                val state by vm.scanState.collectAsState()

                // OnDeviceResult with needsCloud is the intermediate "second
                // opinion in flight" state, not a navigation trigger.
                LaunchedEffect(state) {
                    val destinationRoute =
                        when (val s = state) {
                            is ScanState.OnDeviceResult if (!s.needsCloud) -> Screen.ResultSupported.scanRoute()
                            is ScanState.CloudResult -> Screen.ResultSupported.scanRoute()
                            is ScanState.Unsupported -> Screen.ResultUnsupported.scanRoute()
                            is ScanState.Saved -> Screen.History.route
                            else -> null
                        }
                    if (destinationRoute != null) {
                        navController.navigate(destinationRoute) {
                            popUpTo(Screen.Camera.route)
                            launchSingleTop = true
                        }
                    }
                }

                when (val s = state) {
                    is ScanState.Failed -> Column(Modifier.padding(24.dp)) {
                        Text(s.message)
                        Button(onClick = vm::retryFailedScan) { Text("Try again") }
                        OutlinedButton(onClick = { navController.popBackStack(Screen.Camera.route, inclusive = false) }) {
                            Text("Choose another photo")
                        }
                    }
                    is ScanState.Scanning, ScanState.Idle ->
                        AnalyzingScreen(step = AnalyzingStep.ON_DEVICE)
                    is ScanState.OnDeviceResult ->
                        AnalyzingScreen(
                            step =
                                if (s.needsCloud) {
                                    AnalyzingStep.SECOND_OPINION
                                } else {
                                    AnalyzingStep.ON_DEVICE
                                },
                        )
                    else -> AnalyzingScreen(step = AnalyzingStep.SECOND_OPINION)
                }
            }

            composable(
                route = Screen.ResultSupported.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry ->
                val state by vm.scanState.collectAsState()
                val history by vm.history.collectAsState()
                val recordId = entry.arguments?.getLong("recordId") ?: 0L
                val record = if (recordId != 0L) {
                    history.firstOrNull { it.id == recordId }
                } else {
                    when (val s = state) {
                        is ScanState.OnDeviceResult -> s.record
                        is ScanState.CloudResult -> s.record
                        is ScanState.Unsupported -> s.record
                        else -> null
                    }
                }
                LaunchedEffect(record?.id, record?.isSupported) {
                    if (record != null && !record.isSupported) {
                        navController.navigate(Screen.ResultUnsupported.historyRoute(record.id)) {
                            popUpTo(Screen.Camera.route)
                            launchSingleTop = true
                        }
                    }
                }
                val verificationState by vm.cloudVerificationState.collectAsState()
                val verificationForRecord = verificationState?.takeIf { it.recordId == record?.id }
                if (record != null) {
                    ResultSupportedScreen(
                        record = record,
                        needsCloud = recordId == 0L && state is ScanState.CloudResult,
                        isCloudVerificationRunning = verificationForRecord?.isRunning == true,
                        cloudVerificationMessage = verificationForRecord?.message,
                        onVerifyWithCloudClick = { vm.verifyWithCloud(record) },
                        onSaveClick = { navController.navigate(Screen.History.route) },
                        onTrackClick = {
                            vm.toggleTracked(record)
                            navController.navigate(Screen.MyPlants.route)
                        },
                        onCorrectClick = { correctedSpecies, correctedLabel ->
                            vm.correctDiagnosis(record, correctedSpecies, correctedLabel)
                        },
                    )
                }
            }

            composable(
                route = Screen.ResultUnsupported.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry ->
                val state by vm.scanState.collectAsState()
                val history by vm.history.collectAsState()
                val recordId = entry.arguments?.getLong("recordId") ?: 0L
                val historyRecord = if (recordId != 0L) history.firstOrNull { it.id == recordId } else null
                val unsupported = state as? ScanState.Unsupported
                val rec = if (recordId != 0L) historyRecord else unsupported?.record
                val displayedUnsupported =
                    if (recordId != 0L && historyRecord != null) {
                        ScanState.Unsupported(
                            species = historyRecord.commonName.ifEmpty { historyRecord.detectedSpecies },
                            record = historyRecord,
                        )
                    } else {
                        unsupported
                    }
                val verificationState by vm.cloudVerificationState.collectAsState()
                val verificationForRecord = verificationState?.takeIf { it.recordId == rec?.id }
                ResultUnsupportedScreen(
                    species =
                        if (recordId != 0L) {
                            historyRecord?.commonName?.ifEmpty { historyRecord.detectedSpecies }.orEmpty()
                        } else {
                            unsupported?.species.orEmpty()
                        },
                    scientificName = rec?.cloudLabel ?: rec?.detectedSpecies ?: "",
                    confidence = rec?.confidence,
                    hasCloudHealthAssessment = rec?.hasHealthAssessment == true,
                    healthAssessmentHealthy = rec?.takeIf { it.hasHealthAssessment }?.isHealthy,
                    healthAssessmentDisease = rec?.diseaseName,
                    isSecondOpinionRunning = verificationForRecord?.isRunning == true,
                    secondOpinionMessage = verificationForRecord?.message,
                    onNotifyClick = {
                        vm.requestV2Notify(displayedUnsupported)
                        navController.navigate(Screen.History.route)
                    },
                    onCareTipsClick = { /* call Trefle API */ },
                    onSecondOpinionClick = {
                        rec?.let(vm::verifyUnsupportedWithCloud)
                    },
                )
            }

            composable(Screen.History.route) {
                val records by vm.history.collectAsState()
                val photoStore: ScanPhotoStore = koinInject()
                HistoryScreen(
                    records = records,
                    resolvePhoto = { record -> record.photoPath?.let(photoStore::file) },
                    onItemClick = { record ->
                        if (record.isSupported) {
                            navController.navigate(Screen.ResultSupported.historyRoute(record.id))
                        } else {
                            navController.navigate(Screen.ResultUnsupported.historyRoute(record.id))
                        }
                    },
                )
            }

            composable(Screen.MyPlants.route) {
                val tracked by vm.trackedPlants.collectAsState()
                MyPlantsScreen(
                    trackedRecords = tracked,
                    onUntrackClick = { vm.untrackSpecies(it) },
                )
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
                    if (!selected) {
                        if (tab.screen == Screen.Camera) {
                            // Return to the existing camera entry so its lifecycle
                            // and CameraX controller resume together.
                            if (!navController.popBackStack(Screen.Camera.route, inclusive = false)) {
                                navController.navigate(Screen.Camera.route) {
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigate(tab.screen.route) {
                                // Camera is the tab stack root. Avoid saving and
                                // restoring tab stacks so returning to Scan cannot
                                // revive a stale destination state.
                                popUpTo(Screen.Camera.route)
                                launchSingleTop = true
                            }
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}