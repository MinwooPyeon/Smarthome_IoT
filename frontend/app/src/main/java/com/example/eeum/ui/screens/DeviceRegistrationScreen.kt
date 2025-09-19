package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.eeum.R
import com.example.eeum.ui.components.CheckHubDialog
import com.example.eeum.ui.components.CheckFloorPlanDialog
import com.example.eeum.ui.theme.*

private enum class DeviceKind { HUB, AIR_CONDITIONER, FAN, TV, BEAM_PROJECTOR, AIR_PURIFIER, LIGHT }

private data class DeviceItem(val kind: DeviceKind, val title: String, val iconRes: Int)

private val allDevices = listOf(
    DeviceItem(DeviceKind.HUB, "허브", R.drawable.ic_hub),
    DeviceItem(DeviceKind.AIR_CONDITIONER, "에어컨", R.drawable.ic_air_conditioning),
    DeviceItem(DeviceKind.FAN, "선풍기", R.drawable.ic_electric_fan),
    DeviceItem(DeviceKind.TV, "텔레비전", R.drawable.ic_television),
    DeviceItem(DeviceKind.BEAM_PROJECTOR, "빔프로젝터", R.drawable.ic_beam_projector),
    DeviceItem(DeviceKind.AIR_PURIFIER, "공기청정기", R.drawable.ic_air_purifier),
    DeviceItem(DeviceKind.LIGHT, "조명", R.drawable.ic_light),
)

@Composable
fun DeviceRegistrationScreen(
    navController: NavController? = null,
    onSelect: (String) -> Unit = { kind -> navController?.navigate("device_registration_complete/$kind") }
) {
    // ViewModels 추가
    val activity = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
    val hubVm: HubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
    val homeVm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
    val context = LocalContext.current
    
    // 대화상자 상태 관리
    val (hubDialogVisible, setHubDialogVisible) = remember { mutableStateOf(false) }
    val (floorplanDialogVisible, setFloorplanDialogVisible) = remember { mutableStateOf(false) }
    
    // 허브 목록 상태 관찰
    val hubList by hubVm.hubList.observeAsState(emptyList())
    val hubLoading by hubVm.isLoading.observeAsState(false)
    val hubError by hubVm.error.observeAsState()
    
    // 평면도 상태 관찰
    val floorplans by homeVm.floorplans.observeAsState(emptyList())
    val selectedHomeId by homeVm.selectedHomeId.observeAsState()
    val primaryHomeId by homeVm.primaryHomeId.observeAsState()
    
    // 허브 체크 함수
    var pendingDeviceKind by remember { mutableStateOf<String?>(null) }
    
    fun checkHubAndProceed(deviceKind: String) {
        pendingDeviceKind = deviceKind
        // 허브 목록 조회
        val homeId = selectedHomeId ?: primaryHomeId ?: 1
        hubVm.getHubs(homeId)
    }
    
    // 허브 목록 조회 결과 처리
    LaunchedEffect(hubList, hubLoading, pendingDeviceKind) {
        if (!hubLoading && pendingDeviceKind != null) {
            if (hubList.isEmpty()) {
                // 로딩이 끝났는데 허브가 비어있으면 허브 대화상자 표시
                setHubDialogVisible(true)
                pendingDeviceKind = null
            } else {
                // 허브가 있으면 평면도 체크
                val homeId = selectedHomeId ?: primaryHomeId ?: 1
                homeVm.fetchUserHomeFloorplans(homeId)
            }
        }
    }
    
    // 평면도 조회 결과 처리
    LaunchedEffect(floorplans, pendingDeviceKind) {
        if (pendingDeviceKind != null && hubList.isNotEmpty()) {
            if (floorplans.isEmpty()) {
                // 평면도가 비어있으면 평면도 대화상자 표시
                setFloorplanDialogVisible(true)
                pendingDeviceKind = null
            } else {
                // 평면도가 있으면 다음 단계로 진행
                pendingDeviceKind?.let { deviceKind ->
                    onSelect(deviceKind)
                    pendingDeviceKind = null
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 60.dp)
    ) {
        // 헤더
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_page_move_left),
                contentDescription = "뒤로가기",
                colorFilter = ColorFilter.tint(Gray800),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable { navController?.popBackStack() }
            )
            Text(
                text = "디바이스 등록",
                color = Gray900,
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(60.dp))

// 본문 카드 (AlarmManageScreen과 동일하게 Surface + tonalElevation)
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "디바이스 종류를 선택하세요",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansbold)),
                        color = Gray800
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "부착할 디바이스의 종류를 선택해주세요.",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray600
                    )
                )

                Spacer(Modifier.height(24.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1) 허브는 한 줄 전체를 차지
                    item(span = { GridItemSpan(2) }) {
                        val activity = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
                        val regVm: DeviceRegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
                        val hubVm: HubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
                        val homeVm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
                        val context = LocalContext.current
                        
                        // HomeViewModel 상태 관찰
                        val selectedHomeId by homeVm.selectedHomeId.observeAsState()
                        val primaryHomeId by homeVm.primaryHomeId.observeAsState()
                        
                        
                        DeviceTile(
                            item = allDevices.first { it.kind == DeviceKind.HUB },
                            modifier = Modifier.fillMaxWidth().height(86.dp),
                            onClick = {
                                // 허브 등록 API 호출
                                val currentDraft = regVm.draft.value
                                val homeId = currentDraft?.homeId ?: selectedHomeId ?: primaryHomeId ?: 1
                                val hubDeviceId = "HUB_${System.currentTimeMillis()}" // 임시 생성된 ID
                                
                                hubVm.registerHub(homeId, hubDeviceId)
                                regVm.setKind("HUB")
                                onSelect("HUB")
                            }
                        )
                    }
                    // 2) 나머지 6개 2x3 그리드
                    items(allDevices.filter { it.kind != DeviceKind.HUB }) { item ->
                        val activity = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
                        val regVm: DeviceRegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
                        DeviceTile(
                            item = item,
                            modifier = Modifier.height(86.dp),
                            onClick = {
                                regVm.setKind(item.kind.name)
                                // 허브가 아닌 디바이스는 허브 체크 후 진행
                                checkHubAndProceed(item.kind.name)
                            }
                        )
                    }
                }
            }
        }
        
        // 허브 대화상자
        CheckHubDialog(
            visible = hubDialogVisible,
            onDismiss = { 
                setHubDialogVisible(false)
            },
            onConfirm = {
                setHubDialogVisible(false)
            },
            navController = navController
        )
        
        // 평면도 대화상자
        CheckFloorPlanDialog(
            visible = floorplanDialogVisible,
            onDismiss = { 
                setFloorplanDialogVisible(false)
                navController?.popBackStack() // 이전 화면으로 돌아가기
            },
            onConfirm = {
                setFloorplanDialogVisible(false)
                navController?.popBackStack() // 이전 화면으로 돌아가기
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceTile(
    item: DeviceItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    OutlinedCard(
        modifier = modifier,
        shape = shape,
        border = BorderStroke(1.dp, Gray50),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF9FAFB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { /* TODO: 길게 누르면 온도 다이얼/슬라이더 표시 */ }
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.title,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                    color = Gray800
                ),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Preview
@Composable
private fun DeviceRegistrationPreview() {
    com.example.eeum.ui.theme.EeumTheme(dynamicColor = false) {
        DeviceRegistrationScreen()
    }
}