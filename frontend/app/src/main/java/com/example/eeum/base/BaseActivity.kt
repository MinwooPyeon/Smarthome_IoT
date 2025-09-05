package com.example.mon_fit.base

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

// 액티비티의 기본을 작성, 뷰 바인딩 활용
abstract class BaseActivity<B : ViewBinding>(private val inflate: (LayoutInflater) -> B) :
    AppCompatActivity() {
    protected lateinit var binding: B
        private set

    // 뷰 바인딩 객체를 받아서 inflate해서 화면을 만들어줌.
    // 즉 매번 onCreate에서 setContentView를 하지 않아도 됨.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflate(layoutInflater)
        setContentView(binding.root)
    }

    // 토스트를 쉽게 띄울 수 있게 해줌.
    fun showToast(message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 이미 UI 스레드인 경우 바로 실행
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            // 백그라운드 스레드인 경우 UI 스레드로 전환
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}