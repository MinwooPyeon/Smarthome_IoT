
package com.example.eeum

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
        requestStartupPermissions()
    }

    private fun requestStartupPermissions() {
        val toRequest = buildList {
            add(android.Manifest.permission.RECORD_AUDIO)

            if (Build.VERSION.SDK_INT >= 33) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        perms.ensurePermissions(
            context = this,
            *toRequest,
            onGranted = {
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
                            this@MainActivity, Intent(this@MainActivity, VoiceService::class.java)
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
                    Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다")
            .setMessage("음성 인식을 사용하려면 마이크 권한이 필요합니다. 설정에서 허용해 주세요.")
            .setPositiveButton("설정 열기") { d, _ ->
                d.dismiss()
                PermissionRequester.openAppSettings(this)
            }
            .setNegativeButton("취소") { d, _ -> d.dismiss() }
            .show()
    }
}

