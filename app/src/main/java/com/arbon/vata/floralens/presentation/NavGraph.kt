package com.arbon.vata.floralens.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

@Composable
fun PlantDoctorNavGraph(
    vm: PlantViewModel = koinViewModel()
) {
    val navController = rememberNavController()


    NavHost(navController = navController, startDestination = Screen.Onboarding.route) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onSeePlantsClick = { navController.navigate(Screen.Supported.route) },
                onStartScanClick = { navController.navigate(Screen.Camera.route) }
            )
        }

        composable(Screen.Supported.route) {
            SupportedPlantsScreen(
                onGotItClick = { navController.navigate(Screen.Camera.route) },
                onNotifyBanana = { /* vm.box.put(notify) */ },
                onNotifyAvocado = { }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onShutterClick = {
                    navController.navigate(Screen.Analyzing.route)
                    // In real app: vm.scan(bitmap, path) called from CameraX callback
                    // For preview: simulate scan
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
                    // vm.box.put(wantsNotify=true)
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
            HistoryScreen(
                onItemClick = { record ->
                    if(record.isSupported) navController.navigate(Screen.ResultSupported.route)
                    else navController.navigate(Screen.ResultUnsupported.route)
                }
            )
        }

        composable(Screen.MyPlants.route) {
            MyPlantsScreen()
        }
    }
}