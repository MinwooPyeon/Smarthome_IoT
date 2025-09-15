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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.example.eeum.ui.theme.EeumTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.util.FusedLocationSource

@Composable
fun MapScreen(
    onBack: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp)
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

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = {
                Text(
                    text = "아파트, 오피스텔, 주소 검색",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp,
                    overflow = TextOverflow.Clip,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
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
            NaverMapViewComposable()
        }
    }
}

@Composable
private fun NaverMapViewComposable() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember { MapView(context) }

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
                    setupNaverMap(naverMap, context)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    )
}

private fun setupNaverMap(
    naverMap: NaverMap,
    context: Context
) {
    // 0) 기본 카메라 위치: 36.107113, 128.416401 (초기 디폴트)
    val defaultPos = LatLng(36.107113, 128.416401)
    naverMap.moveCamera(CameraUpdate.scrollTo(defaultPos))
    naverMap.moveCamera(CameraUpdate.zoomTo(18.0))

    // 1) 경량/심볼 및 UI 설정
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

    // 2) 권한 없으면 기본 위치만 보여주고 끝
    if (!hasAnyLocationPermission(context)) {
        naverMap.locationSource = null
        naverMap.locationTrackingMode = LocationTrackingMode.None
        naverMap.locationOverlay.isVisible = false
        Log.w("NaverMap", "Location permission not granted.")
        return
    }

    // 3) 위치 소스/트래킹 모드
    val locationSource = FusedLocationSource(activity, /*REQUEST_CODE*/ 1000)
    naverMap.locationSource = locationSource
    naverMap.locationTrackingMode = LocationTrackingMode.Face
    naverMap.locationOverlay.isVisible = true

    // 4) 첫 실제 위치 수신 시 1회만 현재 위치로 이동
    var cameraInitialized = false
    naverMap.addOnLocationChangeListener { loc ->
        if (!cameraInitialized) {
            val here = LatLng(loc.latitude, loc.longitude)
            naverMap.moveCamera(CameraUpdate.scrollTo(here))
            naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
            cameraInitialized = true
            naverMap.locationTrackingMode = LocationTrackingMode.Face
            Log.d("NaverMap", "Camera moved by onLocationChange: $here")
        }
    }

    // 5) FusedLocationProvider로 즉시 점프(퍼미션 체크/예외 처리 포함)
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
                        naverMap.locationTrackingMode = LocationTrackingMode.Face
                        Log.d("NaverMap", "Camera moved by getCurrentLocation: $here")
                    }
                }
                .addOnFailureListener { e -> Log.e("NaverMap", "getCurrentLocation failed", e) }

            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && !cameraInitialized) {
                    val here = LatLng(loc.latitude, loc.longitude)
                    naverMap.moveCamera(CameraUpdate.scrollTo(here))
                    naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
                    cameraInitialized = true
                    naverMap.locationTrackingMode = LocationTrackingMode.Face
                    Log.d("NaverMap", "Camera moved by lastLocation: $here")
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

// Context → ComponentActivity 안전 추출
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
