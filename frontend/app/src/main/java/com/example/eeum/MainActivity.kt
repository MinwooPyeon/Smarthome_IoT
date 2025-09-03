
package com.example.eeum

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import com.example.eeum.ui.navigation.EeumApp
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.util.PermissionRequester

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
                // 모든 권한 OK → 이후 작업(예: 핫워드 서비스 시작)
                // startForegroundService(Intent(this, VoiceService::class.java))
            },
            onDenied = { denied, permanentlyDenied ->
                // 여기서 UI는 네 선택: 스낵바/토스트/다이얼로그 등
                if (permanentlyDenied.isNotEmpty()) {
                    // "다시 묻지 않음" → 설정으로 유도
                    showGoToSettingsDialog()
                } else {
                    // 단순 거부 → 필요 시 다시 시도 안내
                    // Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
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