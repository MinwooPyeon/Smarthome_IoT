package com.example.eeum.util

import android.content.Context
import android.os.Build
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import com.example.eeum.data.model.dto.voice.ContextBlock
import com.example.eeum.data.model.dto.voice.Grammar
import com.google.gson.JsonObject
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
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

        val compact = t.replace("[,\\s]".toRegex(), "")

        val dayMap = mapOf(
            "월요일" to 0, "월" to 0,
            "화요일" to 1, "화" to 1,
            "수요일" to 2, "수" to 2,
            "목요일" to 3, "목" to 3,
            "금요일" to 4, "금" to 4,
            "토요일" to 5, "토" to 5,
            "일요일" to 6, "일" to 6
        )

        var mask = 0
        var idx = 0
        while (idx < compact.length) {
            var matched = false

            for ((token, bit) in listOf(
                "월요일" to 0, "화요일" to 1, "수요일" to 2, "목요일" to 3, "금요일" to 4, "토요일" to 5, "일요일" to 6,
                "월" to 0, "화" to 1, "수" to 2, "목" to 3, "금" to 4, "토" to 5, "일" to 6
            )) {
                if (compact.startsWith(token, idx)) {
                    mask = mask or (1 shl bit)
                    idx += token.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                idx++
            }
        }

        return if (mask == 0) null else mask
    }

    fun parseTime(raw: String): String? {
        val t = raw.replace("\\s+".toRegex(), " ").trim()
        val am = t.contains("오전") || t.contains("아침") || t.contains("새벽")
        val pm = t.contains("오후") || t.contains("저녁") || t.contains("밤")

        // 1) "8시 반"
        Regex("""(\d{1,2})\s*시\s*반""").find(t)?.let { m ->
            var h = m.groupValues[1].toInt()
            var mm = 30
            if (pm && h in 1..11) h += 12
            if (am && h == 12) h = 0
            if (h !in 0..23) return null
            return "%02d:%02d".format(h, mm)
        }

        // 2) "8시" 또는 "8시 15분"
        Regex("""(\d{1,2})\s*시(?:\s*(\d{1,2})\s*분)?""").find(t)?.let { m ->
            var h = m.groupValues[1].toInt()
            val mm = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: 0
            if (pm && h in 1..11) h += 12
            if (am && h == 12) h = 0
            if (h !in 0..23 || mm !in 0..59) return null
            return "%02d:%02d".format(h, mm)
        }

        return null
    }

    fun toIsoActTime(hhmm: String): String {
        return try {
            val (h, m) = hhmm.split(":").map { it.toInt() }
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val dt = now.withHour(h).withMinute(m).withSecond(0).withNano(0)
            dt.format(DateTimeFormatter.ISO_INSTANT)
        } catch (_: Exception) {
            ZonedDateTime.now(ZoneOffset.UTC)
                .withSecond(0).withNano(0)
                .format(DateTimeFormatter.ISO_INSTANT)
        }
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