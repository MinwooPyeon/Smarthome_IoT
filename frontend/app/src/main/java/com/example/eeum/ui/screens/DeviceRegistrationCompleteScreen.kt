package com.example.eeum.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.base.ApplicationClass
import com.example.eeum.ui.navigation.Tab
import com.example.eeum.R
import com.example.eeum.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import kotlin.math.min
import kotlin.math.roundToInt



@Composable
fun DeviceRegistrationCompleteScreen(
    navController: NavController? = null,
    kind: String? = null,
    homeId: Int? = null,
    onRegister: () -> Unit = {},
    onPositionChange: (Float, Float, Color?) -> Unit = { _, _, _ -> }
) {
    val iconRes = when (kind) {
        "AIR_CONDITIONER" -> R.drawable.ic_icon_air_conditioning
        "FAN" -> R.drawable.ic_icon_electric_fan
        "TV" -> R.drawable.ic_icon_television
        "BEAM_PROJECTOR" -> R.drawable.ic_icon_beam_projector
        "AIR_PURIFIER" -> R.drawable.ic_icon_air_purifier
        "LIGHT" -> R.drawable.ic_icon_light
        "HUB" -> null
        else -> null
    }

    // HomeViewModel에서 평면도 이미지 가져오기 (Activity 범위로 공유)
    val activity = LocalContext.current as ComponentActivity
    val homeVm: HomeViewModel = viewModel(activity)
    val floorplans by homeVm.floorplans.observeAsState(emptyList())
    val selectedHomeId by homeVm.selectedHomeId.observeAsState()
    
    // ViewModel에 위치/색상 반영하기 위한 Activity 범위 VM
    val regVm: DeviceRegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
    val hubVm: HubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)

    // 라우트로 받은 homeId가 있으면 우선 지정
    LaunchedEffect(homeId) {
        homeId?.let { 
            homeVm.selectHome(it)
            regVm.setHomeId(it)
        }
    }
    // 선택된 집이 정해지면 해당 집의 평면도 로드 및 등록 VM에 homeId 설정
    LaunchedEffect(selectedHomeId) {
        selectedHomeId?.let { 
            homeVm.fetchUserHomeFloorplans(it)
            regVm.setHomeId(it)
        }
    }

    val ctx = LocalContext.current
    val imageUrl = floorplans.firstOrNull()?.imageUrl
    val absoluteUrl: String? = remember(imageUrl) { 
        if (imageUrl.isNullOrBlank()) null
        else {
            val base = ApplicationClass.SERVER_URL.trimEnd('/')
            val path = imageUrl.trim()
            if (path.startsWith("http://") || path.startsWith("https://")) path
            else if (path.startsWith("/")) "$base$path" 
            else "$base/$path"
        }
    }

    // 등록 성공 시 토스트 + 새로고침 신호 + Device 탭 이동
    val status by regVm.status.observeAsState()
    LaunchedEffect(status) {
        if (status == "SUCCESS") {
            Toast.makeText(ctx, "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            // Device 탭 새로고침 신호 저장
            try {
                val entry = navController?.getBackStackEntry("main_tabs")
                entry?.savedStateHandle?.set("device_refresh", System.currentTimeMillis())
            } catch (e: Exception) {
                Log.d("디바이스 등록", "DeviceRegistrationCompleteScreen: ${e.message}")
            }
            // Device 탭으로 이동
            navController?.navigate("main_tabs?tab=${Tab.Device.route}") {
                launchSingleTop = true
                popUpTo("main_tabs") { inclusive = false }
            }
        }
    }

    // 허브 등록 성공 처리
    val hubRegStatus by hubVm.registrationStatus.observeAsState()
    val hubError by hubVm.error.observeAsState()
    LaunchedEffect(hubRegStatus) {
        if (kind == "HUB" && hubRegStatus == "SUCCESS") {
            Toast.makeText(ctx, "허브 등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            try {
                val entry = navController?.getBackStackEntry("main_tabs")
                entry?.savedStateHandle?.set("device_refresh", System.currentTimeMillis())
            } catch (e: Exception) {
                Log.d("허브 등록", "DeviceRegistrationCompleteScreen: ${e.message}")
            }
            navController?.navigate("main_tabs?tab=${Tab.Device.route}") {
                launchSingleTop = true
                popUpTo("main_tabs") { inclusive = false }
            }
        }
    }
    LaunchedEffect(hubError) {
        if (kind == "HUB" && hubError != null) {
            Toast.makeText(ctx, "허브 등록 실패: $hubError", Toast.LENGTH_LONG).show()
        }
    }

    // 로드된 평면도 비트맵과 현재 아이콘 위치의 색상 상태
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
    val sampledColorState = remember { mutableStateOf<Color?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 60.dp)
    ) {
        // 헤더: 뒤로가기 + 중앙 타이틀 (다른 화면과 동일 패턴)
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

        Text(
            text = "평면도",
            style = TextStyle(
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.goormsansbold)),
                color = Gray800
            )
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            // 부모 박스 크기 측정 + 드래그 가능한 아이콘
            val density = LocalDensity.current
            val iconOuterSize = 28.dp
            val iconOuterPx = with(density) { iconOuterSize.toPx() }
            var parentSize = remember { mutableStateOf(IntSize.Zero) }
            var iconOffset = remember { mutableStateOf(Offset(Float.NaN, Float.NaN)) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // 카드 내부에 패딩 추가
                    .onSizeChanged { size ->
                        parentSize.value = size
                        if (iconOffset.value.x.isNaN()) {
                            // 최초에는 이미지 중앙에 배치 (이미지가 로드되면 다시 조정)
                            val cx = (size.width - iconOuterPx) / 2f
                            val cy = (size.height - iconOuterPx) / 2f
                            iconOffset.value = Offset(cx, cy)
                        }
                    }
            ) {
                // 서버에서 불러온 평면도 이미지 표시
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(absoluteUrl)
                        .crossfade(true)
                        .allowHardware(false) // 색상 샘플링(getPixel) 위해 소프트웨어 비트맵 사용
                        .build(),
                    contentDescription = "평면도",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onSuccess = { success ->
                        val bmp = (success.result.drawable as? BitmapDrawable)?.bitmap
                        bitmapState.value = bmp
                        
                        // 이미지 로드 완료 시 아이콘을 이미지 영역 중앙으로 이동
                        if (bmp != null && parentSize.value.width > 0 && parentSize.value.height > 0) {
                            val pw = parentSize.value.width.toFloat()
                            val ph = parentSize.value.height.toFloat()
                            val iw = bmp.width.toFloat()
                            val ih = bmp.height.toFloat()
                            val scale = min(pw / iw, ph / ih)
                            val sw = iw * scale
                            val sh = ih * scale
                            val left = (pw - sw) / 2f
                            val top = (ph - sh) / 2f
                            
                            // 이미지 중앙에 아이콘 배치
                            val centerX = left + (sw - iconOuterPx) / 2f
                            val centerY = top + (sh - iconOuterPx) / 2f
                            iconOffset.value = Offset(centerX, centerY)
                        }
                    }
                )
                if (iconRes != null && !iconOffset.value.x.isNaN()) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(iconOuterSize)
                            .offset {
                                IntOffset(
                                    iconOffset.value.x.toInt(),
                                    iconOffset.value.y.toInt()
                                )
                            }
                            .pointerInput(parentSize.value, iconOuterPx, bitmapState.value) {
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        // 드래그 시작 시에도 현재 좌표/색상 보고
                                        val cx = iconOffset.value.x + iconOuterPx / 2f
                                        val cy = iconOffset.value.y + iconOuterPx / 2f
                                        val bmp = bitmapState.value
                                        val color =
                                            if (bmp != null && parentSize.value.width > 0 && parentSize.value.height > 0) {
                                                val pw = parentSize.value.width.toFloat()
                                                val ph = parentSize.value.height.toFloat()
                                                val iw = bmp.width.toFloat()
                                                val ih = bmp.height.toFloat()
                                                val scale = min(pw / iw, ph / ih)
                                                val sw = iw * scale
                                                val sh = ih * scale
                                                val left = (pw - sw) / 2f
                                                val top = (ph - sh) / 2f
                                                if (cx >= left && cx < left + sw && cy >= top && cy < top + sh) {
                                                    val bx = ((cx - left) / scale).roundToInt()
                                                        .coerceIn(0, bmp.width - 1)
                                                    val by = ((cy - top) / scale).roundToInt()
                                                        .coerceIn(0, bmp.height - 1)
                                                    val argb = runCatching {
                                                        bmp.getPixel(
                                                            bx,
                                                            by
                                                        )
                                                    }.getOrNull()
                                                    if (argb != null) {
                                                        Color(
                                                            red = ((argb shr 16) and 0xFF) / 255f,
                                                            green = ((argb shr 8) and 0xFF) / 255f,
                                                            blue = (argb and 0xFF) / 255f,
                                                            alpha = ((argb ushr 24) and 0xFF) / 255f
                                                        )
                                                    } else null
                                                } else null
                                            } else null
                                        sampledColorState.value = color
                                        // 이미지 비트맵 좌표로 정규화하여 전송
                                        val bitmap = bitmapState.value
                                        if (bitmap != null && parentSize.value.width > 0 && parentSize.value.height > 0) {
                                            val pw = parentSize.value.width.toFloat()
                                            val ph = parentSize.value.height.toFloat()
                                            val iw = bitmap.width.toFloat()
                                            val ih = bitmap.height.toFloat()
                                            val scale = min(pw / iw, ph / ih)
                                            val sw = iw * scale
                                            val sh = ih * scale
                                            val left = (pw - sw) / 2f
                                            val top = (ph - sh) / 2f
                                            if (cx >= left && cx < left + sw && cy >= top && cy < top + sh) {
                                                val imagePosX = (cx - left) / scale
                                                val imagePosY = (cy - top) / scale
                                                regVm.setNormalizedPositionAndColor(
                                                    imagePosX,
                                                    imagePosY,
                                                    bitmap.width,
                                                    bitmap.height,
                                                    color
                                                )
                                            } else {
                                                regVm.setPositionAndColor(
                                                    0.5f,
                                                    0.5f,
                                                    color
                                                ) // 이미지 중앙
                                            }
                                        } else {
                                            regVm.setPositionAndColor(0.5f, 0.5f, color) // 디폴트
                                        }
                                        onPositionChange(cx, cy, color)
                                    }
                                ) { _, drag ->
                                    // 이미지 영역 계산 및 드래그 제약
                                    val currentBmp = bitmapState.value
                                    val (nx, ny) = if (currentBmp != null && parentSize.value.width > 0 && parentSize.value.height > 0) {
                                        val pw = parentSize.value.width.toFloat()
                                        val ph = parentSize.value.height.toFloat()
                                        val iw = currentBmp.width.toFloat()
                                        val ih = currentBmp.height.toFloat()
                                        val scale = min(pw / iw, ph / ih)
                                        val sw = iw * scale
                                        val sh = ih * scale
                                        val left = (pw - sw) / 2f
                                        val top = (ph - sh) / 2f
                                        
                                        // 아이콘이 이미지 영역 내에만 위치하도록 제약
                                        val minX = left
                                        val maxX = (left + sw - iconOuterPx).coerceAtLeast(left)
                                        val minY = top
                                        val maxY = (top + sh - iconOuterPx).coerceAtLeast(top)
                                        
                                        val newX = (iconOffset.value.x + drag.x).coerceIn(minX, maxX)
                                        val newY = (iconOffset.value.y + drag.y).coerceIn(minY, maxY)
                                        Pair(newX, newY)
                                    } else {
                                        // 이미지가 로드되지 않은 경우 기존 방식 사용
                                        val maxX = (parentSize.value.width - iconOuterPx).coerceAtLeast(0f)
                                        val maxY = (parentSize.value.height - iconOuterPx).coerceAtLeast(0f)
                                        val newX = (iconOffset.value.x + drag.x).coerceIn(0f, maxX)
                                        val newY = (iconOffset.value.y + drag.y).coerceIn(0f, maxY)
                                        Pair(newX, newY)
                                    }
                                    
                                    iconOffset.value = Offset(nx, ny)

                                    // 아이콘 중앙 좌표(px)
                                    val cx = nx + iconOuterPx / 2f
                                    val cy = ny + iconOuterPx / 2f

                                    // 이미지 위 좌표와 색상 샘플링
                                    val samplingBmp = bitmapState.value
                                    val color =
                                        if (samplingBmp != null && parentSize.value.width > 0 && parentSize.value.height > 0) {
                                            val pw = parentSize.value.width.toFloat()
                                            val ph = parentSize.value.height.toFloat()
                                            val iw = samplingBmp.width.toFloat()
                                            val ih = samplingBmp.height.toFloat()
                                            val scale = min(pw / iw, ph / ih)
                                            val sw = iw * scale
                                            val sh = ih * scale
                                            val left = (pw - sw) / 2f
                                            val top = (ph - sh) / 2f
                                            if (cx >= left && cx < left + sw && cy >= top && cy < top + sh) {
                                                val bx = ((cx - left) / scale).roundToInt()
                                                    .coerceIn(0, samplingBmp.width - 1)
                                                val by = ((cy - top) / scale).roundToInt()
                                                    .coerceIn(0, samplingBmp.height - 1)
                                                val argb =
                                                    runCatching { samplingBmp.getPixel(bx, by) }.getOrNull()
                                                if (argb != null) {
                                                    Color(
                                                        red = ((argb shr 16) and 0xFF) / 255f,
                                                        green = ((argb shr 8) and 0xFF) / 255f,
                                                        blue = (argb and 0xFF) / 255f,
                                                        alpha = ((argb ushr 24) and 0xFF) / 255f
                                                    )
                                                } else null
                                            } else null
                                        } else null
                                    sampledColorState.value = color
                                    // 이미지 비트맵 좌표로 정규화하여 전송
                                    val positionBitmap = bitmapState.value
                                    if (positionBitmap != null && parentSize.value.width > 0 && parentSize.value.height > 0) {
                                        val pw = parentSize.value.width.toFloat()
                                        val ph = parentSize.value.height.toFloat()
                                        val iw = positionBitmap.width.toFloat()
                                        val ih = positionBitmap.height.toFloat()
                                        val scale = min(pw / iw, ph / ih)
                                        val sw = iw * scale
                                        val sh = ih * scale
                                        val left = (pw - sw) / 2f
                                        val top = (ph - sh) / 2f
                                        if (cx >= left && cx < left + sw && cy >= top && cy < top + sh) {
                                            val imagePosX = (cx - left) / scale
                                            val imagePosY = (cy - top) / scale
                                            regVm.setNormalizedPositionAndColor(
                                                imagePosX,
                                                imagePosY,
                                                positionBitmap.width,
                                                positionBitmap.height,
                                                color
                                            )
                                        } else {
                                            regVm.setPositionAndColor(0.5f, 0.5f, color) // 이미지 중앙
                                        }
                                    } else {
                                        regVm.setPositionAndColor(0.5f, 0.5f, color) // 디폴트
                                    }
                                    onPositionChange(cx, cy, color)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (kind == "HUB") {
                    val d = regVm.draft.value
                    val hubDeviceId = d?.serial?.trim().orEmpty()
                    val hId = d?.homeId ?: homeId ?: selectedHomeId ?: 1
                    if (hubDeviceId.isEmpty()) {
                        Toast.makeText(ctx, "허브 시리얼(디바이스 ID)을 먼저 입력/스캔해주세요", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onRegister()
                    hubVm.registerHub(hId, hubDeviceId)
                } else {
                    onRegister()
                    // 등록 요청: DeviceRegistrationViewModel의 draft를 사용
                    regVm.registerCurrentDraft()
                }
                // 이동은 위 LaunchedEffect(status/hubRegStatus)에서 처리
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "등록",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium))
                )
            )
        }
    }
}


@Preview
@Composable
private fun DeviceRegistrationCompletePreview() {
    EeumTheme(dynamicColor = false) {
        DeviceRegistrationCompleteScreen(kind = "AIR_CONDITIONER")
    }
}
