package com.example.eeum.ui.screens

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eeum.data.model.response.floorplans.HouseItem
import com.example.eeum.ui.theme.EeumTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource

@Composable
fun MapScreen(
    onBack: () -> Unit = {},
    vm: FloorplansViewModel = viewModel()
) {
    val isPreview = LocalInspectionMode.current
    var searchText by remember { mutableStateOf("") }

    // ViewModel의 결과 관찰
    val houses: List<HouseItem> by vm.houses.observeAsState(initial = emptyList())

    // 검색어가 바뀔 때마다 조회 (빈문자면 전체 조회)
    LaunchedEffect(searchText) {
        vm.searchHouses(searchText.ifBlank { null })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp, bottom = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "평면도 선택",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Text(
            text = "아래 지도에서 우리집을 선택해주세요.",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // === OutlinedTextField: 잘림 방지 적용 ===
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },

            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                lineHeight = 14.sp, // 폰트 크기와 동일
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            placeholder = {
                Text(
                    text = "아파트, 오피스텔, 주소 검색",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp), // 고정 height 대신 최소 높이로 여유 확보
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "검색",
                    tint = Color.Gray
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color(0xFFEDEDED)),
                contentAlignment = Alignment.Center
            ) { Text("네이버 지도", color = Color.Gray) }
        } else {
            NaverMapViewComposable(houses = houses)
        }
    }
}

@Composable
private fun NaverMapViewComposable(houses: List<HouseItem>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember { MapView(context) }
    val latestHouses by rememberUpdatedState(newValue = houses)

    // addressId 기준으로 마커/인포윈도우 관리
    val markersByAddressId = remember { mutableStateMapOf<Int, Marker>() }
    val infoWindowsByAddressId = remember { mutableStateMapOf<Int, InfoWindow>() }
    var naverMapRef by remember { mutableStateOf<NaverMap?>(null) }

    // 수명주기 연결
    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { naverMap ->
                    naverMapRef = naverMap
                    setupNaverMap(naverMap, context)
                    updateHouseMarkersWithInfoWindows(
                        context,
                        naverMap,
                        markersByAddressId,
                        infoWindowsByAddressId,
                        latestHouses
                    )
                }
            }
        },
        update = {
            val map = naverMapRef
            if (map != null) {
                updateHouseMarkersWithInfoWindows(
                    context,
                    map,
                    markersByAddressId,
                    infoWindowsByAddressId,
                    latestHouses
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    )
}

private fun updateHouseMarkersWithInfoWindows(
    context: Context,
    naverMap: NaverMap,
    markersByAddressId: MutableMap<Int, Marker>,
    infoWindowsByAddressId: MutableMap<Int, InfoWindow>,
    houses: List<HouseItem>
) {
    // 1) 들어온 목록에 없는 addressId는 마커+인포윈도우 제거
    val incomingIds = houses.map { it.addressId }.toSet()
    val toRemove = markersByAddressId.keys.filter { it !in incomingIds }
    toRemove.forEach { id ->
        infoWindowsByAddressId[id]?.close()
        infoWindowsByAddressId.remove(id)

        markersByAddressId[id]?.map = null
        markersByAddressId.remove(id)
    }

    // 2) 추가/갱신
    houses.forEach { house ->
        val existingMarker = markersByAddressId[house.addressId]
        val existingInfo = infoWindowsByAddressId[house.addressId]

        if (existingMarker != null) {
            // 마커 갱신
            existingMarker.position = LatLng(house.latitude, house.longitude)
            // 캡션은 비워서 중복 표시 방지 (InfoWindow만 표시)
            existingMarker.captionText = ""
            if (existingMarker.tag != house.addressId) existingMarker.tag = house.addressId
            if (existingMarker.map == null) existingMarker.map = naverMap

            // 인포윈도우 갱신: 최신 텍스트 반환하도록 어댑터 업데이트
            if (existingInfo != null) {
                existingInfo.adapter = object : InfoWindow.DefaultTextAdapter(context) {
                    override fun getText(window: InfoWindow): CharSequence = house.homeName
                }
                if (existingInfo.marker == null || existingInfo.marker != existingMarker) {
                    existingInfo.open(existingMarker)
                } else {
                    // 이미 열려 있으면 강제로 다시 열어 최신 텍스트 반영
                    existingInfo.close()
                    existingInfo.open(existingMarker)
                }
            } else {
                // 없으면 새로 만들어 연결
                val info = InfoWindow().apply {
                    adapter = object : InfoWindow.DefaultTextAdapter(context) {
                        override fun getText(window: InfoWindow): CharSequence = house.homeName
                    }
                }
                info.open(existingMarker) // 항상 열기
                infoWindowsByAddressId[house.addressId] = info
            }
        } else {
            // 새 마커 생성
            val m = Marker().apply {
                position = LatLng(house.latitude, house.longitude)
                // 캡션은 사용하지 않고 InfoWindow만 사용
                captionText = ""
                // addressId 저장
                tag = house.addressId
                // 마지막에 맵 부착
                map = naverMap
            }
            markersByAddressId[house.addressId] = m

            // 새 인포윈도우 생성 및 항상 열기
            val info = InfoWindow().apply {
                adapter = object : InfoWindow.DefaultTextAdapter(context) {
                    override fun getText(window: InfoWindow): CharSequence = house.homeName
                }
            }
            info.open(m)
            infoWindowsByAddressId[house.addressId] = info
        }
    }

    Log.d(
        "NaverMap",
        "Markers=${markersByAddressId.size}, InfoWindows=${infoWindowsByAddressId.size}"
    )
}

private fun setupNaverMap(
    naverMap: NaverMap,
    context: Context
) {
    val defaultPos = LatLng(36.107113, 128.416401)
    naverMap.moveCamera(CameraUpdate.scrollTo(defaultPos))
    naverMap.moveCamera(CameraUpdate.zoomTo(18.0))

    // POI 심볼은 감추되 마커/InfoWindow는 표시
    naverMap.symbolScale = 0f
    // 라이트 모드에서도 InfoWindow는 잘 보이지만, 필요 시 끄고 테스트 가능
    naverMap.isLiteModeEnabled = true

    naverMap.uiSettings.apply {
        isCompassEnabled = false
        isScaleBarEnabled = false
        isZoomControlEnabled = false
        isIndoorLevelPickerEnabled = false
        isLocationButtonEnabled = hasAnyLocationPermission(context)
        isLogoClickEnabled = false
    }

    val activity = context.findComponentActivity() ?: return

    if (!hasAnyLocationPermission(context)) {
        naverMap.locationSource = null
        naverMap.locationTrackingMode = LocationTrackingMode.None
        naverMap.locationOverlay.isVisible = false
        Log.w("NaverMap", "Location permission not granted.")
        return
    }

    val locationSource = FusedLocationSource(activity, /*REQUEST_CODE*/ 1000)
    naverMap.locationSource = locationSource
    naverMap.locationTrackingMode = LocationTrackingMode.Face
    naverMap.locationOverlay.isVisible = true

    var cameraInitialized = false
    naverMap.addOnLocationChangeListener { loc ->
        if (!cameraInitialized) {
            val here = LatLng(loc.latitude, loc.longitude)
            naverMap.moveCamera(CameraUpdate.scrollTo(here))
            naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
            cameraInitialized = true
        }
    }

    runCatching {
        val fused = LocationServices.getFusedLocationProviderClient(activity)
        val cts = CancellationTokenSource()

        val fineGranted = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!(fineGranted || coarseGranted)) return@runCatching

        try {
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null && !cameraInitialized) {
                        val here = LatLng(loc.latitude, loc.longitude)
                        naverMap.moveCamera(CameraUpdate.scrollTo(here))
                        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
                        cameraInitialized = true
                    }
                }
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && !cameraInitialized) {
                    val here = LatLng(loc.latitude, loc.longitude)
                    naverMap.moveCamera(CameraUpdate.scrollTo(here))
                    naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
                    cameraInitialized = true
                }
            }
        } catch (se: SecurityException) {
            Log.e("NaverMap", "Location access SecurityException", se)
        }
    }.onFailure { Log.e("NaverMap", "FusedLocation init error", it) }
}

private fun hasAnyLocationPermission(ctx: Context): Boolean {
    val fine = ActivityCompat.checkSelfPermission(
        ctx, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ActivityCompat.checkSelfPermission(
        ctx, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    return null
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    EeumTheme(dynamicColor = false) { MapScreen() }
}
