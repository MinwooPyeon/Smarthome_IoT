package com.example.eeum.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eeum.R
import com.example.eeum.data.model.response.home.Home
import com.example.eeum.ui.theme.EeumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMap: () -> Unit = {},
    onAddHome: () -> Unit = {},
    vm: HomeViewModel = viewModel()
) {
    // 🔹 서버 데이터 연결
    val homes: List<Home> by vm.homes.observeAsState(emptyList())

    // 첫 진입 시 서버에서 목록 로드
    LaunchedEffect(Unit) { vm.fetchUserHomes() }

    // 드롭다운에 표시할 문자열 목록
    val homeNames = remember(homes) { homes.map { it.homeName } }

    // 선택 상태(문자열) — 서버 목록이 갱신되면 보정
    var selectedHome by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(homeNames) {
        if (homeNames.isNotEmpty()) {
            if (selectedHome == null || !homeNames.contains(selectedHome)) {
                selectedHome = homeNames.first()
            }
        } else {
            selectedHome = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Greeting("제니님!")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-5).dp)
            ) {
                IconButton(onClick = { /* TODO: settings */ }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "설정",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF475569)
                    )
                }
                IconButton(onClick = { /* TODO: notifications */ }) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "알림",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF475569)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        StatsRow()
        Spacer(Modifier.height(24.dp))

        // 헤더 텍스트
        FloorplanHeader(
            title = "우리 집 평면도",
            onAddClick = { /* 필요 시 추가 */ }
        )

        // ‘집 선택’
        Spacer(Modifier.height(8.dp))
        HomeDropdown(
            selected = selectedHome,
            items = homeNames,
            onSelect = { selectedHome = it },
            onAddNew = onAddHome
        )
        // ▲▲ 드롭박스 끝 ▲▲

        Spacer(Modifier.height(12.dp))
        FloorplanCard(
            onCardClick = onOpenMap
        )
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDropdown(
    selected: String?,                // ⬅️ nullable 로 변경
    items: List<String>,
    onSelect: (String) -> Unit,
    onAddNew: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && items.isNotEmpty() } // 목록 없을 때는 열리지 않게
    ) {
        TextField(
            value = selected ?: "집을 선택하세요",
            onValueChange = {},
            readOnly = true,
            label = { Text("집 선택") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { home ->
                DropdownMenuItem(
                    text = { Text(home, fontSize = 14.sp) },
                    onClick = {
                        onSelect(home)
                        expanded = false
                    }
                )
            }

            Divider()

            DropdownMenuItem(
                leadingIcon = {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                },
                text = {
                    Text(
                        text = "새 집 추가",
                        fontSize = 14.sp,
                        color = Color(0xFF007AFF)
                    )
                },
                onClick = {
                    expanded = false
                    onAddNew()
                }
            )
        }
    }
}

@Composable
private fun TopRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* TODO: notifications */ }) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "알림",
                tint = Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun Greeting(name: String) {
    Text(
        text = name,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF0F172A)
    )
}

@Composable
private fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            title = "조명",
            subtitle = "3개 켜짐",
            iconResource = R.drawable.ic_light,
            tint = Color(0xFFFACC15),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "전력량",
            subtitle = "250kWh",
            iconResource = R.drawable.ic_energy,
            tint = Color(0xFFF97316),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "활성 기기 수",
            subtitle = "4개 가동",
            iconResource = R.drawable.ic_device,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconResource: Int? = null,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                iconResource != null -> Icon(
                    painter = painterResource(id = iconResource),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
        }
    }
}

@Composable
private fun FloorplanHeader(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_move),
            contentDescription = "이동",
            tint = Color(0xFF0F172A)
        )
    }
}

@Composable
private fun FloorplanCard(onCardClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "추가",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeScreenPreview() {
    EeumTheme(dynamicColor = false) {
        HomeScreen()
    }
}
