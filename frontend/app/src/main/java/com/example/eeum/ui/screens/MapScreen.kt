package com.example.eeum.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
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
import com.example.eeum.util.LocalPermissionRequester
import com.example.eeum.util.PermissionRequester
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.Marker
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
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
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
            ) {
                Text("네이버 지도", color = Color.Gray)
            }
        } else {
            NaverMapViewComposable()
        }
    }
}

@Composable
private fun NaverMapViewComposable() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionRequester = LocalPermissionRequester.current

    // SDK Client 보장 주입 (Manifest 메타데이터에서 읽어 MapView 생성 전에 1회 설정)
    remember {
        runCatching {
            val ai = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val cid = ai.metaData?.getString("com.naver.maps.map.CLIENT_ID")
            require(!cid.isNullOrBlank()) { "NAVER CLIENT_ID is blank" }
            NaverMapSdk.getInstance(context).client =
                NaverMapSdk.NaverCloudPlatformClient(cid!!)
            Log.d("NaverMap", "CLIENT_ID set from Manifest: $cid")
        }.onFailure {
            Log.e("NaverMap", "Failed to set CLIENT_ID", it)
        }
        true
    }

    // MapView는 한 번만 생성
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
                    setupNaverMap(
                        naverMap = naverMap,
                        context = context,
                        permissionRequester = permissionRequester
                    )
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
    context: Context,
    permissionRequester: PermissionRequester?
) {
    // 기본 카메라(서울시청)
    val defaultPos = CameraPosition(
        LatLng(37.5666805, 126.9784147),
        16.0
    )
    naverMap.moveCamera(CameraUpdate.toCameraPosition(defaultPos))

    // UI 설정
    naverMap.uiSettings.apply {
        isLocationButtonEnabled = true
        isCompassEnabled = true
        isScaleBarEnabled = true
        isZoomControlEnabled = true
    }

    val activity = context.findComponentActivity() ?: run {
        naverMap.uiSettings.isLocationButtonEnabled = false
        return
    }

    // Google Play Services Location API 가용성 (누락/미탑재 기기 방지)
    val hasGmsLocation = try {
        Class.forName("com.google.android.gms.location.LocationCallback"); true
    } catch (_: ClassNotFoundException) {
        false
    }
    if (!hasGmsLocation) {
        naverMap.uiSettings.isLocationButtonEnabled = false
        return
    }

    val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 주입된 PermissionRequester 사용
    if (permissionRequester?.hasPermissions(activity, *permissions) == true) {
        enableLocationTracking(naverMap, activity)
    } else {
        permissionRequester?.ensurePermissions(
            context = activity,
            permissions = permissions,
            onGranted = { enableLocationTracking(naverMap, activity) },
            onDenied = { _, _ -> naverMap.uiSettings.isLocationButtonEnabled = false }
        ) ?: run {
            naverMap.uiSettings.isLocationButtonEnabled = false
        }
    }
}

private fun enableLocationTracking(naverMap: NaverMap, activity: Activity) {
    val locationSource = FusedLocationSource(activity, 1000)
    naverMap.locationSource = locationSource
    naverMap.locationTrackingMode = LocationTrackingMode.NoFollow

    runCatching {
        val fused = LocationServices.getFusedLocationProviderClient(activity)
        val fineGranted = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return@runCatching

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val here = LatLng(loc.latitude, loc.longitude)
                naverMap.moveCamera(CameraUpdate.scrollTo(here))
                naverMap.moveCamera(CameraUpdate.zoomTo(16.0))
                Marker().apply {
                    position = here
                    map = naverMap
                }
            }
        }
    }
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    EeumTheme(dynamicColor = false) {
        MapScreen()
    }
}
