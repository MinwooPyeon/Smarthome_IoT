package com.example.eeum.ui.navigation

import androidx.compose.foundation.layout.*
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
import com.example.eeum.ui.screens.*
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
private const val PASSWORD_CHANGE_ROUTE = "password_change"

private const val MAP_ROUTE = "map"

// ✅ addressId를 경로 파라미터로 넘기는 평면도 선택 화면
private const val SELECT_FLOORPLAN_ROUTE = "select_floorplan/{addressId}"
private fun selectFloorplanRoute(addressId: Int) = "select_floorplan/$addressId"

@Composable
fun EeumApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MAIN_TABS_ROUTE
    ) {
        // 1) BottomNavigation 포함 영역
        composable(MAIN_TABS_ROUTE) {
            MainTabsScreen(navController)
        }

        // 2) BottomNavigation 미포함 화면들
        composable(LOGIN_ROUTE) { /* LoginScreen() */ }

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

        // ✅ addressId를 받아서 SelectFloorplanScreen으로 전달
        composable(
            route = SELECT_FLOORPLAN_ROUTE,
            arguments = listOf(navArgument("addressId") { type = NavType.IntType })
        ) { backStackEntry ->
            val addressId = backStackEntry.arguments?.getInt("addressId") ?: 0
            SelectFloorplanScreen(
                addressId = addressId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(PASSWORD_CHANGE_ROUTE) { PasswordChangeScreen(navController) }
    }
}

// BottomNavigation이 포함된 메인 탭 화면들을 관리하는 컴포저블
@Composable
private fun MainTabsScreen(mainNavController: androidx.navigation.NavController) {
    val tabNavController = rememberNavController()

    M2Scaffold(
        isFloatingActionButtonDocked = true,
        floatingActionButtonPosition = M2FabPosition.Center,
        backgroundColor = Color.Transparent,
        floatingActionButton = {
            EeumFloatingActionButton(
                onClick = {
                    mainNavController.navigate(VOICE_ROUTE) {
                        launchSingleTop = true
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
            composable(Tab.Device.route) { DeviceScreen() }
            composable(Tab.Use.route) { EnergyScreen() }
            composable(Tab.Menu.route) { MenuScreen(mainNavController) }
        }
    }
}
