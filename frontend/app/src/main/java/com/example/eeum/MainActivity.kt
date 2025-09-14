package com.example.eeum

import com.example.eeum.BuildConfig // ← 여기 확인!
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.eeum.base.DeviceDirectoryCache
import com.example.eeum.data.remote.RetrofitUtil
import com.example.eeum.ui.navigation.EeumApp
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.util.PermissionRequester
import com.example.eeum.util.VoiceDeps
import com.example.eeum.voice.VoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "EEUM_MainActivity"

class MainActivity : ComponentActivity() {

    private val perms by lazy { PermissionRequester.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EeumTheme(dynamicColor = false) {
                EeumApp()
            }
        }
        requestStartupPermissions() // 앱 시작 시 모든 권한(마이크+알림+위치) 요청
    }

    private fun requestStartupPermissions() {
        val toRequest = buildList {
            // 음성
            add(android.Manifest.permission.RECORD_AUDIO)
            // 알림 (T+)
            if (Build.VERSION.SDK_INT >= 33) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            // 지도용 위치 권한(포그라운드) — 배경 위치는 여기서 요청하지 않음
            add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }.toTypedArray()

        perms.ensurePermissions(
            context = this,
            *toRequest,
            onGranted = {
                // 권한 OK → 기존 로직 유지
                lifecycleScope.launch {
                    try {
                        val dir = DeviceDirectoryCache(RetrofitUtil.deviceService)
                        val count = withContext(Dispatchers.IO) {
                            runCatching { dir.loadAllDevices() }.getOrElse { -1 }
                        }

                        if (count <= 0 && BuildConfig.DEBUG) {
                            val seed = mapOf(
                                "방1 에어컨" to 10,
                                "거실 전등" to 20,
                                "거실2 선풍기" to 30,
                                "방2 선풍기" to 40,
                                "방1 선풍기" to 50
                            )
                            val seeded = DeviceDirectoryCache.withSeed(
                                RetrofitUtil.deviceService, seed
                            )
                            VoiceDeps.directory = seeded
                            Log.w(TAG, "requestStartupPermissions: 로드 실패/빈 결과 → 디버그 시드 적용")
                        } else {
                            VoiceDeps.directory = dir
                            Log.d(TAG, "requestStartupPermissions: 디바이스 $count 개 로드됨")
                        }

                        ContextCompat.startForegroundService(
                            this@MainActivity,
                            Intent(this@MainActivity, VoiceService::class.java)
                        )
                    } catch (e: Exception) {
                        Log.d(TAG, "requestStartupPermissions: ERROR - ${e.message}")
                    }
                }
            },
            onDenied = { _, permanentlyDenied ->
                if (permanentlyDenied.isNotEmpty()) {
                    showGoToSettingsDialog()
                } else {
                    Toast.makeText(this, "필수 권한이 거부되어 일부 기능이 제한됩니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다")
            .setMessage("마이크/알림/위치 권한을 허용해야 전체 기능을 사용할 수 있습니다. 설정에서 허용해 주세요.")
            .setPositiveButton("설정 열기") { d, _ ->
                d.dismiss()
                PermissionRequester.openAppSettings(this)
            }
            .setNegativeButton("취소") { d, _ -> d.dismiss() }
            .show()
    }
}
