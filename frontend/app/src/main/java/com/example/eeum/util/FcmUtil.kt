package com.example.eeum.util

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging


object FcmUtil {
    fun subscribeHomeTopic(homeId: Int) {
        FirebaseMessaging.getInstance()
            .subscribeToTopic("home-$homeId")
            .addOnCompleteListener { t ->
                Log.d("FcmUtil", "subscribe home-$homeId ok=${t.isSuccessful}")
            }
    }

    fun unsubscribeHomeTopic(homeId: Int) {
        FirebaseMessaging.getInstance()
            .unsubscribeFromTopic("home-$homeId")
            .addOnCompleteListener { t ->
                Log.d("FcmUtil", "unsubscribe home-$homeId ok=${t.isSuccessful}")
            }
    }

    /** 1회 토큰 조회 → 콜백으로 넘김(서버 업로드는 콜백에서 처리) */
    fun uploadFcmTokenOnce(onToken: (String) -> Unit = {}) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let(onToken)
                } else {
                    Log.w("FcmUtil", "token fetch failed", task.exception)
                }
            }
    }
}
