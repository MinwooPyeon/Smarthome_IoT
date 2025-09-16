package com.example.eeum.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eeum.R
import com.example.eeum.ui.components.EeumBottomAppBar
import com.example.eeum.ui.components.EeumFloatingActionButton
import com.example.eeum.ui.screens.DeviceScreen
import com.example.eeum.ui.screens.HomeScreen
import com.example.eeum.ui.screens.MenuScreen
import com.example.eeum.ui.screens.EnergyScreen
import com.example.eeum.ui.screens.VoiceScreen
import com.example.eeum.ui.screens.RoutineScreen
import com.example.eeum.ui.screens.CreateRoutineFirstScreen
import com.example.eeum.ui.screens.CreateRoutineSecondScreen
import com.example.eeum.ui.screens.LoginScreen
import com.example.eeum.ui.screens.LogManageScreen
import com.example.eeum.ui.screens.AlarmManageScreen
import com.example.eeum.ui.screens.MapScreen
import com.example.eeum.ui.screens.UserInformationScreen
import com.example.eeum.ui.screens.PasswordChangeScreen
import com.example.eeum.ui.screens.DeviceRegistrationScreen
import com.example.eeum.ui.screens.DeviceRegistrationQRScreen
import com.example.eeum.ui.screens.DeviceRegistrationCompleteScreen

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
private const val DEVICE_REGISTRATION_ROUTE = "device_registration"
private const val DEVICE_REGISTRATION_QR_ROUTE = "device_registration_qr"
private const val DEVICE_REGISTRATION_COMPLETE_ROUTE = "device_registration_complete"

private const val MAP_ROUTE = "map"

@Composable
fun EeumApp() {
    val navController = rememberNavController()

    // 메인 NavHost - BottomNavigation 유무를 결정하는 최상위 네비게이션
    NavHost(
        navController = navController,
        startDestination = MAIN_TABS_ROUTE  // 임시로 메인탭부터 시작 (나중에 LOGIN_ROUTE로 변경 가능)
    ) {
        // 1️⃣ BottomNavigation이 포함된 화면들 (메인 앱 화면)
        composable(MAIN_TABS_ROUTE) {
            MainTabsScreen(navController)
        }

        // 2️⃣ BottomNavigation이 없는 화면들
        composable(LOGIN_ROUTE) {
            LoginScreen()
        }

        composable(LOG_MANAGE_ROUTE) {
            LogManageScreen(navController)
        }

        composable(ALARM_MANAGE_ROUTE) {
            AlarmManageScreen(navController)
        }

        composable(VOICE_ROUTE) {
            VoiceScreen()
        }

        composable(ROUTINE_ROUTE) {
            RoutineScreen(navController)
        }

        composable(ROUTE_CREATE_ROUTINE_FIRST) {
            CreateRoutineFirstScreen(navController)
        }

        composable(ROUTE_CREATE_ROUTINE_SECOND) {
            CreateRoutineSecondScreen(navController)
        }

        composable(USER_INFORMATION_ROUTE) {
            UserInformationScreen(navController)
        }

        composable(MAP_ROUTE) {
            MapScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(PASSWORD_CHANGE_ROUTE) {
            PasswordChangeScreen(navController)
        }
        // 디바이스 등록 플로우
        composable(DEVICE_REGISTRATION_ROUTE) {
            DeviceRegistrationScreen(navController) { kind ->
                navController.navigate("$DEVICE_REGISTRATION_COMPLETE_ROUTE/$kind") { launchSingleTop = true }
            }
        }
        composable(DEVICE_REGISTRATION_QR_ROUTE) {
            DeviceRegistrationQRScreen(navController)
        }
        composable("$DEVICE_REGISTRATION_COMPLETE_ROUTE/{kind}") { backStackEntry ->
            val kind = backStackEntry.arguments?.getString("kind")
            DeviceRegistrationCompleteScreen(navController, kind)
        }
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
                //BottomAppBar 자체 높이는 그대로 두고,
                //아래에 시스템 내비게이션 바 높이만큼 빈 공간을 추가
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
                    //Spacer가 '밖에' 생기는 여백이라 cutoutShape가 늘어나지 않습니다.
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
