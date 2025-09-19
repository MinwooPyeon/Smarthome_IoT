package com.example.eeum.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eeum.ui.components.EeumBottomAppBar
import com.example.eeum.ui.components.EeumFloatingActionButton
import com.example.eeum.ui.screens.DeviceScreen
import com.example.eeum.ui.screens.EnergyScreen
import com.example.eeum.ui.screens.MenuScreen
import com.example.eeum.ui.screens.RoutineScreen
import com.example.eeum.ui.screens.VoiceScreen
import com.example.eeum.ui.screens.CreateRoutineFirstScreen
import com.example.eeum.ui.screens.CreateRoutineSecondScreen
import com.example.eeum.ui.screens.LogManageScreen
import com.example.eeum.ui.screens.AlarmManageScreen
import com.example.eeum.ui.screens.MapScreen
import com.example.eeum.ui.screens.SelectFloorplanScreen
import com.example.eeum.ui.screens.UserInformationScreen
import com.example.eeum.ui.screens.DeviceRegistrationScreen
import com.example.eeum.ui.screens.DeviceRegistrationQRScreen
import com.example.eeum.ui.screens.DeviceRegistrationSerialScreen
import com.example.eeum.ui.screens.DeviceRegistrationBrandScreen
import com.example.eeum.ui.screens.DeviceRegistrationCompleteScreen
import com.example.eeum.ui.screens.HomeScreen
import com.example.eeum.ui.screens.RemoteScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eeum.ui.screens.HomeViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity

import androidx.compose.material.Scaffold as M2Scaffold
import androidx.compose.material.FabPosition as M2FabPosition

// BottomNavigation이 있는 화면들의 라우트
private const val MAIN_TABS_ROUTE = "main_tabs"
private const val VOICE_ROUTE = "voice"

// BottomNavigation이 없는 화면들의 라우트
private const val LOGIN_ROUTE = "login"
private const val LOG_MANAGE_ROUTE = "log_manage"
private const val ALARM_MANAGE_ROUTE = "alarm_manage"
private const val ROUTINE_ROUTE = "routine"
private const val ROUTE_CREATE_ROUTINE_FIRST = "createRoutineFirst"
private const val ROUTE_CREATE_ROUTINE_SECOND = "createRoutineSecond"
private const val USER_INFORMATION_ROUTE = "user_information"
private const val DEVICE_REGISTRATION_ROUTE = "device_registration"
private const val DEVICE_REGISTRATION_QR_ROUTE = "device_registration_qr"
private const val DEVICE_REGISTRATION_SERIAL_ROUTE = "device_registration_serial"
private const val DEVICE_REGISTRATION_BRAND_ROUTE = "device_registration_brand"
private const val DEVICE_REGISTRATION_COMPLETE_ROUTE = "device_registration_complete"

private const val REMOTE_ROUTE = "remote"
private const val MAP_ROUTE = "map"

// addressId를 경로 파라미터로 넘기는 평면도 선택 화면
private const val SELECT_FLOORPLAN_ROUTE = "select_floorplan/{addressId}"
private fun selectFloorplanRoute(addressId: Int) = "select_floorplan/$addressId"

@Composable
fun EeumApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MAIN_TABS_ROUTE
    ) {
        // 1) BottomNavigation 포함 메인
        composable(
            route = "$MAIN_TABS_ROUTE?tab={tab}",
            arguments = listOf(
                navArgument("tab") { defaultValue = Tab.Home.route }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("tab")
            MainTabsScreen(navController, initialTab = initialTab)
        }

        // 2) BottomNavigation 미포함 화면들
        composable(LOGIN_ROUTE) { /* TODO: Login screen */ }

        composable(LOG_MANAGE_ROUTE) { LogManageScreen(navController) }
        composable(ALARM_MANAGE_ROUTE) { AlarmManageScreen(navController) }

        composable(VOICE_ROUTE) { VoiceScreen() }

        composable(ROUTINE_ROUTE) { RoutineScreen(navController) }
        composable(ROUTE_CREATE_ROUTINE_FIRST) { CreateRoutineFirstScreen(navController) }
        composable(ROUTE_CREATE_ROUTINE_SECOND) { CreateRoutineSecondScreen(navController) }

        composable(USER_INFORMATION_ROUTE) { UserInformationScreen(navController) }

        composable(MAP_ROUTE) {
            MapScreen(
                onBack = { navController.popBackStack() },
                onSelectAddress = { id ->
                    navController.navigate(selectFloorplanRoute(id)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(REMOTE_ROUTE) { RemoteScreen(navController) }

        composable(
            route = SELECT_FLOORPLAN_ROUTE,
            arguments = listOf(navArgument("addressId") { type = NavType.IntType })
        ) { backStackEntry ->
            val addressId = backStackEntry.arguments?.getInt("addressId") ?: 0
            SelectFloorplanScreen(
                addressId = addressId,
                onBack = { navController.popBackStack() },
                onNavigateHome = {
                    // 등록 성공 시 메인 탭(Home)으로 이동
                    navController.popBackStack(MAIN_TABS_ROUTE, inclusive = false)
                }
            )
        }

        // 디바이스 등록 플로우
        composable(DEVICE_REGISTRATION_ROUTE) {
            DeviceRegistrationScreen(navController) { kind ->
                navController.navigate("$DEVICE_REGISTRATION_QR_ROUTE/$kind") {
                    launchSingleTop = true
                }
            }
        }

        composable("$DEVICE_REGISTRATION_QR_ROUTE/{kind}") { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind")
            // Note: Cannot acquire a ViewModel here (not a @Composable scope for remember). Pass simple lambda only.
            DeviceRegistrationQRScreen(
                navController = navController,
                kind = kind,
                onManualInput = {
                    navController.navigate("$DEVICE_REGISTRATION_SERIAL_ROUTE/$kind") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("$DEVICE_REGISTRATION_SERIAL_ROUTE/{kind}") { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind")
            // 현재 선택된 homeId를 가져와 완료 화면으로 전달 (Activity 범위의 VM)
            val activity = LocalContext.current as ComponentActivity
            val homeVm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
            val regVm: com.example.eeum.ui.screens.DeviceRegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
            DeviceRegistrationSerialScreen(navController) { serial ->
                regVm.setSerial(serial)
                val homeId = homeVm.selectedHomeId.value
                if (kind == "HUB") {
                    val query = homeId?.let { "?homeId=$it" } ?: ""
                    navController.navigate("$DEVICE_REGISTRATION_COMPLETE_ROUTE/$kind$query") {
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate("$DEVICE_REGISTRATION_BRAND_ROUTE/$kind?serial=$serial") {
                        launchSingleTop = true
                    }
                }
            }
        }

        composable("$DEVICE_REGISTRATION_BRAND_ROUTE/{kind}?serial={serial}") { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind")
            val activity = LocalContext.current as ComponentActivity
            val homeVm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
            val regVm: com.example.eeum.ui.screens.DeviceRegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
            DeviceRegistrationBrandScreen(navController) { payload ->
                // payload: "brand|model" 형태
                val parts = payload.split("|")
                if (parts.size >= 2) {
                    regVm.setBrandModel(parts[0], parts[1])
                }
                val homeId = homeVm.selectedHomeId.value
                val query = homeId?.let { "?homeId=$it" } ?: ""
                navController.navigate("$DEVICE_REGISTRATION_COMPLETE_ROUTE/$kind$query") {
                    launchSingleTop = true
                }
            }
        }

        composable(
            route = "$DEVICE_REGISTRATION_COMPLETE_ROUTE/{kind}?homeId={homeId}",
            arguments = listOf(
                navArgument("homeId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind")
            val homeId = backStackEntry.arguments?.getInt("homeId")?.let { if (it == -1) null else it }
            val lastLogTime = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
            DeviceRegistrationCompleteScreen(
                navController = navController,
                kind = kind,
                homeId = homeId,
                onPositionChange = { x, y, color ->
                    val now = System.currentTimeMillis()
                    if (now - lastLogTime.value >= 50L) {
                        val hex = color?.let {
                            val a = (it.alpha * 255f).toInt().coerceIn(0, 255)
                            val r = (it.red   * 255f).toInt().coerceIn(0, 255)
                            val g = (it.green * 255f).toInt().coerceIn(0, 255)
                            val b = (it.blue  * 255f).toInt().coerceIn(0, 255)
                            String.format("#%02X%02X%02X%02X", a, r, g, b) // ARGB
                        } ?: "null"
                        android.util.Log.d("DeviceRegComplete", "x=$x, y=$y, color=$hex")
                        lastLogTime.value = now
                    }
                }
            )
        }
        composable("$DEVICE_REGISTRATION_COMPLETE_ROUTE/{kind}") { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind")
            DeviceRegistrationCompleteScreen(navController, kind)
        }
    }
}

/** BottomNavigation 이 포함된 메인 탭 화면 */
@Composable
private fun MainTabsScreen(
    mainNavController: androidx.navigation.NavController,
    initialTab: String? = null
) {
    val tabNavController = rememberNavController()

    // 초기 탭 이동 (기본 Home)
    androidx.compose.runtime.LaunchedEffect(initialTab) {
        val target = initialTab ?: Tab.Home.route
        if (target != Tab.Home.route) {
            tabNavController.navigate(target) { launchSingleTop = true }
        }
    }

    M2Scaffold(
        isFloatingActionButtonDocked = true,
        floatingActionButtonPosition = M2FabPosition.Center,
        backgroundColor = Color.Transparent,
        floatingActionButton = {
            EeumFloatingActionButton(
                onClick = {
                    mainNavController.navigate(VOICE_ROUTE) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(tabNavController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                EeumBottomAppBar(
                    currentDestination = currentDestination,
                    onTabClick = { route ->
                        tabNavController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(tabNavController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                )
                // 시스템 내비게이션바 높이만큼만 추가 여백
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    onOpenMap = { mainNavController.navigate(MAP_ROUTE) }
                )
            }
            composable(Tab.Device.route) { DeviceScreen(mainNavController) }
            composable(Tab.Use.route) { EnergyScreen() }
            composable(Tab.Menu.route) { MenuScreen(mainNavController) }
        }
    }
}
