package com.example.eeum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eeum.ui.theme.EeumTheme

// === M2 컴포넌트는 별칭으로 import (M3와 이름 충돌 방지) ===
import androidx.compose.material.Scaffold as M2Scaffold
import androidx.compose.material.FabPosition as M2FabPosition
import androidx.compose.material.BottomAppBar
import androidx.compose.ui.Alignment
import androidx.compose.material.FloatingActionButton as M2FAB
import androidx.compose.material.Icon as M2Icon
import androidx.compose.material.Text as M2Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EeumTheme(dynamicColor = false) { EeumAppMaterial() } }
    }
}

private enum class Tab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Home("home", "홈", Icons.Outlined.Home),
    Device("device", "디바이스", Icons.Outlined.AddCircle),
    Use("use", "사용량", Icons.Outlined.DateRange),
    Menu("menu", "메뉴", Icons.Outlined.Menu)
}

@Composable
private fun EeumAppMaterial() {
    val navController = rememberNavController()

    // 배경: drawable/background.xml 적용 (paint 사용)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(id = R.drawable.background), // ← drawable/background.xml
                contentScale = ContentScale.Crop
            )
    ) {
        M2Scaffold(
            // 도킹 FAB + 자동 컷아웃
            isFloatingActionButtonDocked = true,
            floatingActionButtonPosition = M2FabPosition.Center,
            backgroundColor = Color.Transparent, // 배경 보이도록 투명

            floatingActionButton = {
                M2FAB(
                    onClick = { /* TODO */ },
                    shape = CircleShape,
                    backgroundColor = Color(0xFF6BB6FF),
                    contentColor = Color.Black,
                    elevation = androidx.compose.material.FloatingActionButtonDefaults.elevation(12.dp)
                ) {
                    M2Icon(Icons.Filled.Add, contentDescription = "추가")
                }
            },

            bottomBar = {
                BottomAppBar(
                    cutoutShape = CircleShape,       // FAB 모양으로 자동 컷아웃
                    backgroundColor = Color.White,
                    contentColor = Color.Gray,
                    elevation = 8.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 좌측 2개
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Tab.entries.take(2).forEach { tab ->
                                BottomItemM2(
                                    tab = tab,
                                    selected = currentDestination.isOnRoute(tab.route),
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.width(72.dp))

                        // 우측 2개
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Tab.entries.drop(2).forEach { tab ->
                                BottomItemM2(
                                    tab = tab,
                                    selected = currentDestination.isOnRoute(tab.route),
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Tab.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Tab.Home.route) { HomeScreen() }
                composable(Tab.Device.route) { DeviceScreen() }
                composable(Tab.Use.route) { UseScreen() }
                composable(Tab.Menu.route) { MenuScreen() }
            }
        }
    }
}
@Composable
private fun BottomItemM2(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selColor = MaterialTheme.colorScheme.primary
    val dimColor = Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .widthIn(min = 56.dp)
            .clickable(onClick = onClick)
    ) {
        M2Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (selected) selColor else dimColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        M2Text(
            text = tab.label,
            fontSize = 12.sp,
            color = if (selected) selColor else dimColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

private fun NavDestination?.isOnRoute(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true

// 이하 화면들은 기존 그대로
@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("홈 화면", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("메인 콘텐츠가 여기에 표시됩니다", fontSize = 16.sp, color = Color.Gray)
        }
    }
}

@Composable
fun DeviceScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("디바이스 화면", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("연결된 디바이스 목록이 여기에 표시됩니다", fontSize = 16.sp, color = Color.Gray)
        }
    }
}

@Composable
fun UseScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("사용량 화면", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("전력 사용량 통계가 여기에 표시됩니다", fontSize = 16.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MenuScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("메뉴 화면", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("설정 및 기타 옵션이 여기에 표시됩니다", fontSize = 16.sp, color = Color.Gray)
        }
    }
}
