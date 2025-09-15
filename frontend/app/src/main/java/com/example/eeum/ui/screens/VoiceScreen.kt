package com.example.eeum.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.example.eeum.data.model.dto.voice.NluUpdate
import com.example.eeum.util.VoiceBus
import com.example.eeum.voice.VoiceService

@Composable
fun VoiceScreen() {
    val ctx = LocalContext.current
    val update: NluUpdate? by VoiceBus.updates.collectAsStateWithLifecycle(initialValue = null)

    // ---- 마이크 권한 상태 ----
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    // 화면 최초 진입 시 1회만 자동 시작
    var autoStarted by remember { mutableStateOf(false) }

    // 권한이 생기는 순간 1회 듣기 시작 (자동 시작은 진입 시 1회만)
    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission && !autoStarted) {
            ctx.startService(Intent(ctx, VoiceService::class.java).apply {
                action = VoiceService.ACTION_START_LISTEN
            })
            autoStarted = true
        }
    }

    // 진입/이탈 라이프사이클: 이탈 시 정리
    DisposableEffect(Unit) {
        // 진입 시: 권한 없으면 요청, 있으면 위 LaunchedEffect가 처리
        if (!hasAudioPermission) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        onDispose {
            ctx.startService(Intent(ctx, VoiceService::class.java).apply {
                action = VoiceService.ACTION_STOP_LISTEN
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("음성 인식 화면", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "음성 명령을 말씀해 주세요",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 제어 버튼들
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (hasAudioPermission) {
                        ctx.startService(Intent(ctx, VoiceService::class.java).apply {
                            action = VoiceService.ACTION_START_LISTEN
                        })
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            ) { Text("다시 듣기") }

            OutlinedButton(
                onClick = {
                    ctx.startService(Intent(ctx, VoiceService::class.java).apply {
                        action = VoiceService.ACTION_STOP_LISTEN
                    })
                }
            ) { Text("중지") }
        }

        Divider()

        Text("Raw:", fontWeight = FontWeight.SemiBold)
        Text(update?.raw ?: "대기 중")

        Spacer(Modifier.height(8.dp))
        Text("Parsed intents:", fontWeight = FontWeight.SemiBold)

        if (update?.intents.isNullOrEmpty()) {
            Text("아직 없음")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(update!!.intents) { r ->
                    IntentCard(intent = r.intent, slots = r.slots)
                }
            }
        }
    }
}

@Composable
private fun IntentCard(intent: String, slots: Map<String, String>) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text("Intent: $intent", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (slots.isEmpty()) {
                Text("Slots: (none)")
            } else {
                slots.forEach { (k, v) ->
                    Text("• $k = $v")
                }
            }
        }
    }
}

@Preview
@Composable
private fun VoiceScreenPreview() {
    com.example.eeum.ui.theme.EeumTheme(dynamicColor = false) {
        VoiceScreen()
    }
}
