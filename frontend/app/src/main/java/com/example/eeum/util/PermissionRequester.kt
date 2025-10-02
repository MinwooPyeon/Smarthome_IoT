package com.example.eeum.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class PermissionRequester private constructor(
    private val caller: ActivityResultCaller,
    private val rationaleProvider: (String) -> Boolean
) {
    private lateinit var lastRequested: Array<String>
    private var onGranted: (() -> Unit)? = null
    private var onDenied: ((denied: List<String>, permanentlyDenied: List<String>) -> Unit)? = null

    private val launcher: ActivityResultLauncher<Array<String>> =
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val denied = result.filterValues { !it }.keys.toList()
            if (denied.isEmpty()) {
                onGranted?.invoke()
                return@registerForActivityResult
            }
            val permanentlyDenied = denied.filter { !rationaleProvider(it) }
            onDenied?.invoke(denied, permanentlyDenied)
        }

    // 권한 보유 체크
    fun hasPermissions(context: Context, vararg permissions: String): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    // 런타임 요청 대상으로만 정규화
    fun ensurePermissions(
        context: Context,
        vararg permissions: String,
        onGranted: () -> Unit,
        onDenied: ((denied: List<String>, permanentlyDenied: List<String>) -> Unit)? = null
    ) {
        // 런타임 권한만 걸러서 요청
        val requestables = permissions.filter {
            it != android.Manifest.permission.FOREGROUND_SERVICE &&
                    it != android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE &&
                    !(it == android.Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < 33)
        }.toTypedArray()

        if (hasPermissions(context, *requestables)) {
            onGranted()
            return
        }
        this.onGranted = onGranted
        this.onDenied = onDenied
        lastRequested = requestables
        launcher.launch(requestables)
    }

    companion object {
        fun from(activity: ComponentActivity): PermissionRequester =
            PermissionRequester(
                caller = activity,
                rationaleProvider = { perm ->
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                }
            )

        fun from(fragment: Fragment): PermissionRequester =
            PermissionRequester(
                caller = fragment,
                rationaleProvider = { perm ->
                    fragment.shouldShowRequestPermissionRationale(perm)
                }
            )

        // 영구 거부 시 설정 화면으로 이동할 때 사용
        fun openAppSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
        }
    }
}