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
import com.example.eeum.ui.screens.CreatetRoutineFirstScreen
import com.example.eeum.ui.screens.CreateRoutineSecondScreen
import com.example.eeum.ui.screens.LoginScreen
import com.example.eeum.ui.screens.LogManageScreen
import com.example.eeum.ui.screens.AlarmManageScreen

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

@Composable
fun EeumApp() {
    val navController = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(id = R.drawable.background),
                contentScale = ContentScale.Crop
            )
    ) {
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
                CreatetRoutineFirstScreen(navController)
            }
            
            composable(ROUTE_CREATE_ROUTINE_SECOND) {
                CreateRoutineSecondScreen(navController)
            }
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
                    // 메인 NavController를 사용해서 음성 화면으로 이동 (BottomNavigation 없음)
                    mainNavController.navigate(VOICE_ROUTE) {
                        launchSingleTop = true
                    }
                }
            )
        },
        bottomBar = {
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
        }
    ) { innerPadding ->
        // 탭 전용 NavHost - BottomNavigation이 있는 화면들만 포함
        NavHost(
            navController = tabNavController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Tab.Home.route) { HomeScreen() }
            composable(Tab.Device.route) { DeviceScreen() }
            composable(Tab.Use.route) { EnergyScreen() }
            composable(Tab.Menu.route) { 
                MenuScreen(mainNavController) // 메인 NavController 전달 (다른 화면 이동 시)
            }
        }
    }
}
