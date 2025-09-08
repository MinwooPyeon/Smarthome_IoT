package com.example.eeum.util

import android.content.Context
import androidx.annotation.RawRes
import com.example.eeum.data.model.dto.voice.ContextBlock
import com.example.eeum.data.model.dto.voice.Grammar
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
}