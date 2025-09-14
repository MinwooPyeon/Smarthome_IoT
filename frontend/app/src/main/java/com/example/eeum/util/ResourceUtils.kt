package com.example.eeum.util

import android.content.Context
import androidx.annotation.RawRes
import com.example.eeum.data.model.dto.voice.ContextBlock
import com.example.eeum.data.model.dto.voice.Grammar
import com.google.gson.JsonObject
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStreamReader

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

    fun loadFromAssets(ctx: Context, path: String = "grammar.yml"): Grammar {
        ctx.assets.open(path).use { ins ->
            val yaml = Yaml()
            val map = yaml.load<Map<String, Any?>>(InputStreamReader(ins, Charsets.UTF_8))
            @Suppress("UNCHECKED_CAST")
            val context = (map["context"] as Map<String, Any?>)
            val expressions = context["expressions"] as Map<String, List<String>>
            val slots = (context["slots"] as? Map<String, List<String>>).orEmpty()
            val macros = (context["macros"] as? Map<String, List<String>>).orEmpty()
            return Grammar(ContextBlock(expressions, slots, macros))
        }
    }

    fun parseWeekdays(raw: String): Int? {
        val t = raw.replace("\\s+".toRegex(), " ").trim()
        if (t.contains("매일") || t.contains("항상")) return (1 shl 7) - 1
        if (t.contains("평일")) return (1 shl 5) - 1
        if (t.contains("주말")) return (1 shl 5) or (1 shl 6)
        val map = mapOf(
            "월" to 0, "월요일" to 0,
            "화" to 1, "화요일" to 1,
            "수" to 2, "수요일" to 2,
            "목" to 3, "목요일" to 3,
            "금" to 4, "금요일" to 4,
            "토" to 5, "토요일" to 5,
            "일" to 6, "일요일" to 6
        )
        var mask = 0
        for ((k, bit) in map) {
            if (t.contains(k)) mask = mask or (1 shl bit)
        }
        return mask
    }

    fun parseTime(raw: String): String? {
        val t = raw.replace("\\s+".toRegex(), " ").trim()
        val am = t.contains("오전") || t.contains("아침") || t.contains("새벽")
        val pm = t.contains("오후") || t.contains("저녁") || t.contains("밤")

        val rx = Regex("""(\d{1,2})\s*시(?:\s*(\d{1,2})\s*분)?""")
        val m = rx.find(t) ?: return null
        var h = m.groupValues[1].toInt()
        val mm = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: 0

        if (pm && h in 1..11) h += 12
        if (am && h == 12) h = 0

        if (h !in 0..23 || mm !in 0..59) return null
        return "%02d:%02d".format(h, mm)
    }

    fun friendlyFail(e: Throwable): String {
        val msg = e.message?.trim().orEmpty()
        return if (msg.isNotEmpty()) msg else "요청을 처리하지 못했어요."
    }

    fun JsonObject.optBool(key: String): Boolean? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asBoolean }.getOrNull() else null

    fun JsonObject.optInt(key: String): Int? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asInt }.getOrNull() else null
}