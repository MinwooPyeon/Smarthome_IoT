
package com.example.eeum

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.eeum.ui.navigation.EeumApp
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.util.PermissionRequester
import com.example.eeum.voice.PicovoiceManagerEngine
import com.example.eeum.voice.VoiceService

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
                // 권한 허용 후 서비스 시작
                ContextCompat.startForegroundService(
                    this, Intent(this, VoiceService::class.java)
                )
            },
            onDenied = { denied, permanentlyDenied ->
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