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
    var searchText by remember { mutableStateOf("") }
    val houses: List<HouseItem> by vm.houses.observeAsState(emptyList())

    LaunchedEffect(searchText) {
        vm.searchHouses(searchText.ifBlank { null })
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

        if (isPreview) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFEDEDED)),
                contentAlignment = Alignment.Center
            ) { Text("네이버 지도", color = Color.Gray) }
        } else {
            NaverMapViewComposable(
                houses = houses,
                onMarkerSelected = onSelectAddress
            )
        }
    }
}

@Composable
private fun NaverMapViewComposable(
    houses: List<HouseItem>,
    onMarkerSelected: ((Int) -> Unit)?
) {
    val context = LocalContext.current

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
                    setupNaverMap(map, context)
                    updateMarkers(
                        context, map,
                        markersByAddressId, infoWindowsByAddressId,
                        latestHouses, onMarkerSelected
                    )
                }
            }
        },
        update = {
            naverMapRef?.let { map ->
                updateMarkers(
                    context, map,
                    markersByAddressId, infoWindowsByAddressId,
                    latestHouses, onMarkerSelected
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
            onMarkerSelected?.invoke(house.addressId) // ← 네비게이션 콜백 호출
            true
        }

        val info = infoWindowsByAddressId[house.addressId] ?: InfoWindow().also {
            infoWindowsByAddressId[house.addressId] = it
        }
        info.adapter = object : InfoWindow.DefaultTextAdapter(context) {
            override fun getText(window: InfoWindow): CharSequence = house.homeName
        }
        if (info.marker != marker) info.open(marker)
    }
}

private fun setupNaverMap(naverMap: NaverMap, context: Context) {
    val defaultPos = LatLng(36.107113, 128.416401)
    naverMap.moveCamera(CameraUpdate.scrollTo(defaultPos))
    naverMap.moveCamera(CameraUpdate.zoomTo(18.0))

    naverMap.symbolScale = 0f
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

    val source = FusedLocationSource(activity, 1000)
    naverMap.locationSource = source
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

        val fine = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!(fine || coarse)) return@runCatching

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

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    EeumTheme(dynamicColor = false) { MapScreen() }
}
