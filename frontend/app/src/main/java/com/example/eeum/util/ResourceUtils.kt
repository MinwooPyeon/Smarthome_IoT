package com.example.eeum.util

import android.content.Context
import androidx.annotation.RawRes
import java.io.File

// res/raw 파일을 앱 내부 저장소로 복사해 실제 파일 경로를 제공.
object ResourceUtils {
    fun rawToFile(context: Context, @RawRes resId: Int, outName: String): String {
        val out = File(context.filesDir, outName)
        if (!out.exists()) {
            context.resources.openRawResource(resId).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return out.absolutePath
    }
}