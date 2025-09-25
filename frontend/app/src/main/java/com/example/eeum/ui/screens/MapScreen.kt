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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eeum.data.model.response.floorplans.HouseItem
import com.example.eeum.ui.theme.EeumTheme
import android.view.View
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
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
    onSelectAddress: ((Int) -> Unit)? = null,
    vm: FloorplansViewModel = viewModel()
) {
    val isPreview = LocalInspectionMode.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var searchText by remember { mutableStateOf("") }
    val houses: List<HouseItem> by vm.houses.observeAsState(emptyList())
    var selectedHouseForFocus by remember { mutableStateOf<HouseItem?>(null) }
    var naverMapInstance by remember { mutableStateOf<NaverMap?>(null) }
    var showSearchResults by remember { mutableStateOf(false) }

    LaunchedEffect(searchText) {
        vm.searchHouses(searchText.ifBlank { null })
        showSearchResults = searchText.isNotBlank()
    }
    
    // 선택된 집으로 포커스 이동
    LaunchedEffect(selectedHouseForFocus) {
        selectedHouseForFocus?.let { house ->
            naverMapInstance?.let { map ->
                val targetPos = LatLng(house.latitude, house.longitude)
                map.moveCamera(CameraUpdate.scrollTo(targetPos))
                showSearchResults = false // 리스트 숨기기
                
                // 포커스 해제 및 키보드 숨기기
                focusManager.clearFocus()
                keyboardController?.hide()
                
                selectedHouseForFocus = null // 이동 후 초기화
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.Black,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "평면도 선택", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Text(
            text = "아래 지도에서 우리집을 선택해주세요.",
            fontSize = 16.sp, color = Color.Black, modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                lineHeight = 14.sp,
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
                .heightIn(min = 52.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "검색", tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // 검색 결과 리스트
        if (showSearchResults && houses.isNotEmpty()) {
            SearchResultsList(
                houses = houses,
                onHouseSelected = { house ->
                    selectedHouseForFocus = house
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (isPreview) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFEDEDED)),
                contentAlignment = Alignment.Center
            ) { Text("네이버 지도", color = Color.Gray) }
        } else {
            NaverMapViewComposable(
                houses = houses,
                onMarkerSelected = onSelectAddress,
                onMapReady = { map -> naverMapInstance = map },
                focusManager = focusManager,
                keyboardController = keyboardController
            )
        }
    }
}

// 검색 결과 리스트 컴포너블
@Composable
private fun SearchResultsList(
    houses: List<HouseItem>,
    onHouseSelected: (HouseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .background(
                Color.White,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(houses) { house ->
            SearchResultItem(
                house = house,
                onSelected = { onHouseSelected(house) }
            )
        }
    }
}

// 검색 결과 개별 아이템
@Composable
private fun SearchResultItem(
    house: HouseItem,
    onSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = house.homeName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )
        if (!house.detail.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = house.detail,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun NaverMapViewComposable(
    houses: List<HouseItem>,
    onMarkerSelected: ((Int) -> Unit)?,
    onMapReady: ((NaverMap) -> Unit)? = null,
    focusManager: androidx.compose.ui.focus.FocusManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    val context = LocalContext.current
    var selectedMarkerId by remember { mutableStateOf<Int?>(null) }

    // MapView는 한번만 만들고, 단일 생명주기 제어
    val mapView = remember { MapView(context) }
    var naverMapRef by remember { mutableStateOf<NaverMap?>(null) }
    val markersByAddressId = remember { mutableStateMapOf<Int, Marker>() }
    val infoWindowsByAddressId = remember { mutableStateMapOf<Int, InfoWindow>() }
    val latestHouses by rememberUpdatedState(houses)

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    naverMapRef = map
                    onMapReady?.invoke(map)
                    val onMarkerSelectionChangedCallback = { markerId: Int? -> selectedMarkerId = markerId }
                    setupNaverMap(map, context, onMarkerSelectionChangedCallback, focusManager, keyboardController)
                    updateMarkers(
                        context, map,
                        markersByAddressId, infoWindowsByAddressId,
                        latestHouses, selectedMarkerId,
                        onMarkerSelectionChangedCallback,
                        onMarkerSelected
                    )
                }
            }
        },
        update = {
            naverMapRef?.let { map ->
                updateMarkers(
                    context, map,
                    markersByAddressId, infoWindowsByAddressId,
                    latestHouses, selectedMarkerId,
                    { markerId -> selectedMarkerId = markerId },
                    onMarkerSelected
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun updateMarkers(
    context: Context,
    naverMap: NaverMap,
    markersByAddressId: MutableMap<Int, Marker>,
    infoWindowsByAddressId: MutableMap<Int, InfoWindow>,
    houses: List<HouseItem>,
    selectedMarkerId: Int?,
    onMarkerSelectionChanged: (Int?) -> Unit,
    onMarkerSelected: ((Int) -> Unit)?
) {
    // 제거
    val incoming = houses.map { it.addressId }.toSet()
    val toRemove = markersByAddressId.keys.filter { it !in incoming }
    toRemove.forEach { id ->
        infoWindowsByAddressId[id]?.close()
        infoWindowsByAddressId.remove(id)
        markersByAddressId[id]?.map = null
        markersByAddressId.remove(id)
    }

    // 추가/갱신
    houses.forEach { house ->
        val lat = house.latitude
        val lon = house.longitude

        // 좌표 검증
        val valid = !lat.isNaN() && !lon.isNaN() &&
                lat in -90.0..90.0 && lon in -180.0..180.0
        if (!valid) {
            Log.w("NaverMap", "Skip invalid coord: id=${house.addressId}, lat=$lat lon=$lon")
            return@forEach
        }

        val marker = markersByAddressId[house.addressId] ?: Marker().also {
            markersByAddressId[house.addressId] = it
        }

        // 순서 중요: position 먼저, 그 다음 map
        marker.position = LatLng(lat, lon)
        marker.captionText = ""
        marker.tag = house.addressId
        if (marker.map == null) marker.map = naverMap

        marker.setOnClickListener {
            val newSelection = if (selectedMarkerId == house.addressId) null else house.addressId
            onMarkerSelectionChanged(newSelection)
            // 마커 클릭 시는 선택만 하고 네비게이션은 하지 않음
            true
        }

        val info = infoWindowsByAddressId[house.addressId] ?: InfoWindow().also {
            infoWindowsByAddressId[house.addressId] = it
        }
        
        val isSelected = selectedMarkerId == house.addressId
        info.adapter = createCustomInfoWindowAdapter(
            context = context, 
            house = house, 
            isSelected = isSelected
        )
        
        // InfoWindow 전체 영역 클릭 리스너
        info.setOnClickListener {
            if (!isSelected) {
                // 비선택 상태에서 선택되도록
                onMarkerSelectionChanged(house.addressId)
            } else {
                // 선택된 상태에서 SelectFloorplanScreen으로 이동
                onMarkerSelected?.invoke(house.addressId)
                Log.d("MapScreen", "InfoWindow clicked - navigating to SelectFloorplanScreen for addressId: ${house.addressId}")
            }
            true
        }
        
        if (info.marker != marker) info.open(marker)
    }
}

private fun createCustomInfoWindowAdapter(
    context: Context,
    house: HouseItem,
    isSelected: Boolean
): InfoWindow.ViewAdapter {
    return object : InfoWindow.ViewAdapter() {
        override fun getView(infoWindow: InfoWindow): View {
            val view = if (isSelected) {
                // 선택된 상태: 확장된 내용 표시 (파란색 배경)
android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(32, 24, 32, 24)
                    
                    // 주소 이름
                    addView(TextView(context).apply {
                        text = house.homeName
                        textSize = 16f
                        setTextColor(android.graphics.Color.WHITE)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setPadding(0, 0, 0, 12)
                    })
                    
                    // 디테일 주소 (API에서 받아온 실제 데이터)
                    if (!house.detail.isNullOrEmpty()) {
                        addView(TextView(context).apply {
                            text = house.detail
                            textSize = 13f
                            setTextColor(android.graphics.Color.WHITE)
                            alpha = 0.9f
                            setPadding(0, 0, 0, 16)
                        })
                    }
                    
                    // 평면도 선택 버튼 (Row 레이아웃: 텍스트 + 아이콘)
                    addView(android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        setPadding(0, 8, 0, 0)
                        
                        // 텍스트
                        addView(TextView(context).apply {
                            text = "평면도 선택"
                            textSize = 16f
                            setTextColor(android.graphics.Color.WHITE)
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                1.0f
                            )
                        })
                        
                        // 아이콘 (오른쪽 끝)
                        addView(android.widget.ImageView(context).apply {
                            setImageResource(com.example.eeum.R.drawable.ic_page_move)
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                24.dpToPx(context),
                                24.dpToPx(context)
                            ).apply {
                                gravity = android.view.Gravity.CENTER_VERTICAL
                            }
                            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                            setColorFilter(android.graphics.Color.WHITE)
                        })
                    })
                    
                    // 파란색 배경, 둥근 모서리 설정
                    val drawable = android.graphics.drawable.GradientDrawable()
                    drawable.setColor(android.graphics.Color.parseColor("#4285F4"))
                    drawable.cornerRadius = 24f
                    background = drawable
                }
            } else {
                // 기본 상태: homeName만 표시 (흰색 배경 + 파란색 테두리 + 검은색 글씨)
                TextView(context).apply {
                    text = house.homeName
                    textSize = 14f
                    setTextColor(android.graphics.Color.BLACK)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(32, 20, 32, 20)
                    isClickable = true
                    isFocusable = true
                    
                    // 흰색 배경 + 파란색 테두리
                    val drawable = android.graphics.drawable.GradientDrawable()
                    drawable.setColor(android.graphics.Color.WHITE)
                    drawable.setStroke(4, android.graphics.Color.parseColor("#4285F4"))
                    drawable.cornerRadius = 20f
                    background = drawable
                }
            }
            return view
        }
    }
}

private fun setupNaverMap(
    naverMap: NaverMap, 
    context: Context, 
    onMarkerSelectionChanged: (Int?) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    val defaultPos = LatLng(36.107113, 128.416401)
    naverMap.moveCamera(CameraUpdate.scrollTo(defaultPos))
    naverMap.moveCamera(CameraUpdate.zoomTo(17.0))

    naverMap.symbolScale = 0f
    naverMap.isLiteModeEnabled = true
    
    // 지도 클릭 시 마커 선택 해제 및 포커스 해제
    naverMap.setOnMapClickListener { _, _ ->
        onMarkerSelectionChanged(null)
        focusManager.clearFocus()
        keyboardController?.hide()
    }

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

    val source = FusedLocationSource(activity, 1000)
    naverMap.locationSource = source
    naverMap.locationTrackingMode = LocationTrackingMode.Face
    naverMap.locationOverlay.isVisible = true

    var cameraInitialized = false
    naverMap.addOnLocationChangeListener { loc ->
        if (!cameraInitialized) {
            val here = LatLng(loc.latitude, loc.longitude)
            naverMap.moveCamera(CameraUpdate.scrollTo(here))
            naverMap.moveCamera(CameraUpdate.zoomTo(17.0))
            cameraInitialized = true
        }
    }

    runCatching {
        val fused = LocationServices.getFusedLocationProviderClient(activity)
        val cts = CancellationTokenSource()

        val fine = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!(fine || coarse)) return@runCatching

        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null && !cameraInitialized) {
                    val here = LatLng(loc.latitude, loc.longitude)
                    naverMap.moveCamera(CameraUpdate.scrollTo(here))
                    naverMap.moveCamera(CameraUpdate.zoomTo(17.0))
                    cameraInitialized = true
                }
            }
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && !cameraInitialized) {
                val here = LatLng(loc.latitude, loc.longitude)
                naverMap.moveCamera(CameraUpdate.scrollTo(here))
                naverMap.moveCamera(CameraUpdate.zoomTo(17.0))
                cameraInitialized = true
            }
        }
    }.onFailure { Log.e("NaverMap", "FusedLocation init error", it) }
}

private fun hasAnyLocationPermission(ctx: Context): Boolean {
    val fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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

// dp를 px로 변환하는 확장 함수
private fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    EeumTheme(dynamicColor = false) { MapScreen() }
}
