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
import com.example.eeum.ui.screens.CreateRoutineFirstScreen
import com.example.eeum.ui.screens.DeviceScreen
import com.example.eeum.ui.screens.HomeScreen
import com.example.eeum.ui.screens.MenuScreen
import com.example.eeum.ui.screens.UseScreen
import com.example.eeum.ui.screens.VoiceScreen
import com.example.eeum.ui.screens.RoutineScreen // 기존 루틴 목록/메인
import com.example.eeum.ui.screens.CreateRoutineSecondScreen // 루틴 생성 2단계

import androidx.compose.material.Scaffold as M2Scaffold
import androidx.compose.material.FabPosition as M2FabPosition

private const val VOICE_ROUTE = "voice"
private const val ROUTINE_ROUTE = "routine"

// 내부 전환 전용 라우트(바텀바 노출 X)
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
        M2Scaffold(
            isFloatingActionButtonDocked = true,
            floatingActionButtonPosition = M2FabPosition.Center,
            backgroundColor = Color.Transparent,
            floatingActionButton = {
                EeumFloatingActionButton(
                    onClick = {
                        navController.navigate(VOICE_ROUTE) {
                            launchSingleTop = true
                        }
                    }
                )
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                EeumBottomAppBar(
                    currentDestination = currentDestination,
                    onTabClick = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Tab.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                // 바텀탭
                composable(Tab.Home.route) { HomeScreen() }
                composable(Tab.Device.route) { DeviceScreen() }
                composable(Tab.Use.route) { UseScreen() }
                composable(Tab.Menu.route) { MenuScreen() }
                composable(VOICE_ROUTE) { VoiceScreen() }

                // 루틴 메인 화면
                composable(ROUTINE_ROUTE) { RoutineScreen(navController) }

                //루틴 생성
                composable(ROUTE_CREATE_ROUTINE_FIRST) {
                    CreateRoutineFirstScreen(navController)
                }
                composable(ROUTE_CREATE_ROUTINE_SECOND) {
                    CreateRoutineSecondScreen()
                }
            }
        }
    }
}
