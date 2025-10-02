package com.example.eeum.data.remote.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.eeum.MainActivity
import com.example.eeum.R
import com.example.eeum.util.NotificationUtil
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random


class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.i("FCM", "new token: $token")
        // TODO: 서버에 업로드 (사용자/디바이스 식별자와 함께)
        // EeumApi.uploadFcmToken(token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val title = msg.notification?.title ?: msg.data["title"] ?: getString(R.string.app_name)
        val body  = msg.notification?.body  ?: msg.data["body"]  ?: "알림이 도착했어요"

        val type = msg.data["type"]               // "ROUTINE_EXECUTED" 등
        val routineId = msg.data["routineId"]?.toIntOrNull()
        val routineName = msg.data["routineName"]

        showNotification(title, body, type, routineId, routineName)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String?,
        routineId: Int?,
        routineName: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deeplink_type", type ?: "")
            putExtra("routineId", routineId ?: -1)
            putExtra("routineName", routineName ?: "")
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = PendingIntent.getActivity(this, 0, intent, piFlags)

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        val notif = NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID_DEFAULT)
            .setSmallIcon(R.mipmap.ic_launcher) // 필요하면 @drawable/ic_notification 로 교체
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setLargeIcon(largeIcon)
            .build()

        safeNotify(Random.nextInt(), notif)
    }

    private fun safeNotify(id: Int, notif: Notification) {
        val granted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (granted) {
            try {
                NotificationManagerCompat.from(this).notify(id, notif)
            } catch (se: SecurityException) {
                Log.w("FCM", "notify() blocked by SecurityException", se)
            }
        } else {
            Log.w("FCM", "POST_NOTIFICATIONS not granted; skip notify")
            // TODO: 앱 포그라운드 진입 시 Activity에서 권한 요청 유도
        }
    }
}