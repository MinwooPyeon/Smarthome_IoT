//package com.example.mon_fit.base
//
//import android.annotation.SuppressLint
//import android.util.Log
//import com.example.mon_fit.data.model.response.MotionResponse
//import com.example.mon_fit.wearable.MotionViewModel
//import com.google.gson.Gson
//import io.reactivex.disposables.CompositeDisposable
//import ua.naiksoftware.stomp.Stomp
//import ua.naiksoftware.stomp.StompClient
//import ua.naiksoftware.stomp.dto.LifecycleEvent
//
//object StompManager {
//    private const val SOCKET_URL = "ws://i13d102.p.ssafy.io:8084/ws"
//    private val stompClient: StompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
//    private val disposables = CompositeDisposable()
//    private val gson = Gson()
//
//    private var motionViewModel: MotionViewModel? = null
//
//    fun setMotionViewModel(viewModel: MotionViewModel) {
//        motionViewModel = viewModel
//    }
//
//    @SuppressLint("CheckResult")
//    fun connect() {
//        stompClient.lifecycle().subscribe { event ->
//            when (event.type) {
//                LifecycleEvent.Type.OPENED -> {
//                    Log.d("Stomp", "✅ 연결됨")
//
//                    // ✅ 연결되었을 때만 구독!
//                    val disp = stompClient.topic("/topic/raid/result").subscribe { message ->
//                        Log.d("Stomp", "📥 응답 수신: ${message.payload}")
//
//                        // 받은 메시지를 MotionResponse 객체로 파싱
//                        try {
//                            val motionResponse = gson.fromJson(message.payload, MotionResponse::class.java)
//
//                            // ViewModel에 prediction 값 전달
////                            motionViewModel?.updatePrediction(motionResponse)
//
//                            // Log로 확인 (optionally)
//                            Log.d("Stomp", "Prediction: ${motionResponse}")
//                            Log.d("Stomp", "Prediction: ${motionResponse.motionData}")
//                            Log.d("Stomp", "Prediction: ${motionResponse.motionData?.className}")
//                        } catch (e: Exception) {
//                            Log.e("Stomp", "❌ JSON 파싱 오류: ${e.message}")
//                        }
//                    }
//                    disposables.add(disp)
//                }
//
//                LifecycleEvent.Type.ERROR -> {
//                    Log.e("Stomp", "❌ 연결 실패", event.exception)
//                }
//
//                LifecycleEvent.Type.CLOSED -> {
//                    Log.d("Stomp", "❎ 연결 종료됨")
//                }
//
//                else -> Unit
//            }
//        }
//
//        stompClient.connect()
//    }
//
//
//    fun sendMotionData(json: String) {
//        stompClient.send("/app/raid/motion", json).subscribe({
//            Log.d("Stomp", "📤 전송 성공")
//        }, {
//            Log.e("Stomp", "❌ 전송 실패", it)
//        })
//    }
//
//    fun disconnect() {
//        stompClient.disconnect()
//        disposables.clear()
//    }
//}
