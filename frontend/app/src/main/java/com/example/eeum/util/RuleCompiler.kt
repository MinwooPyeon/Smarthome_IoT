package com.example.eeum.util

import android.util.Log
import com.example.eeum.data.model.dto.voice.CompiledIntent
import com.example.eeum.data.model.dto.voice.Grammar
import com.example.eeum.data.model.dto.voice.PatternRule

private const val TAG = "EEUM_RuleCompiler"

object RuleCompiler {

    fun compile(g: Grammar): List<CompiledIntent> {
        val compiledIntents = mutableListOf<CompiledIntent>()

        g.context.expressions.forEach { (intent, lines) ->
            val rules = lines.mapIndexed { idx, line ->
                // 1) 한 줄 → 정규식 소스로 전개
                val rxSrc = lineToRegex(line, g)

                // 2) 원문 라인에서 등장했던 "키" 수집 ($라벨:키)
                val namesFromLine = collectSlotKeysFromLine(line)

                // 3) 실제 정규식 소스에 (?<키> …) 가 생성된 이름만 필터
                val namesInRegex: Set<String> = namesFromLine.filter { slotKey ->
                    rxSrc.contains("(?<$slotKey>")
                }.toSet()

                // 4) 로깅
                if (namesFromLine != namesInRegex) {
                    Log.d(
                        TAG,
                        "[$intent#$idx] slot mismatch; line='$line'\n" +
                                "  fromLine=$namesFromLine\n" +
                                "  inRegex =$namesInRegex\n" +
                                "  rxSrc   =$rxSrc"
                    )
                } else {
                    Log.d(
                        TAG,
                        "[$intent#$idx] ok; slots=$namesInRegex\n  rxSrc=$rxSrc"
                    )
                }

                // 5) 정규식 컴파일
                val regex = try {
                    Regex(rxSrc, setOf(RegexOption.IGNORE_CASE))
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "REGEX FAIL [$intent#$idx]\n  line ='$line'\n  rxSrc='$rxSrc'",
                        e
                    )
                    throw e
                }

                // 6) 패턴 룰 생성
                PatternRule(regex = regex, slotNames = namesInRegex)
            }

            compiledIntents += CompiledIntent(intent = intent, rules = rules)
        }

        Log.d(TAG, "Compiled ${compiledIntents.size} intents.")
        return compiledIntents
    }

    // ─────────────────────────────────────────────
    // $라벨:키 → '키'만 수집 (캡처 그룹명도 '키' 사용)
    // 라벨: 한글/영문/숫자/_/.(pv.* 지원)
    // ─────────────────────────────────────────────
    private fun collectSlotKeysFromLine(line: String): Set<String> {
        val slotRe = Regex("""\$([A-Za-z0-9_.가-힣]+):([A-Za-z0-9_]+)""")
        return slotRe.findAll(line).map { it.groupValues[2] }.toSet()
    }

    // 메타문자 이스케이프
    private fun reQuoteLiteral(s: String): String {
        val sb = StringBuilder(s.length * 2)
        for (ch in s) {
            when (ch) {
                '\\', '.', '^', '$', '|', '?', '*', '+', '(', ')', '[', ']', '{', '}' -> {
                    sb.append('\\').append(ch)
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    // ★ 한글 음절 사이의 옵션 공백 허용: "해줘" ↔ "해 줘", "빔프로젝터" ↔ "빔 프로젝터"
    private fun flexHangul(token: String): String {
        val esc = reQuoteLiteral(token.trim())
        // 한글-한글 사이에 \s* 주입 (문자열 리터럴을 위해 \\s*)
        return esc.replace(Regex("(?<=[가-힣])(?=[가-힣])"), "\\\\s*")
    }

    // pv.* 내장 패턴 (필요 시 확장)
    private fun pvPatternOf(label: String): String? = when (label) {
        "pv.TwoDigitInteger"    -> """(?:[0-9]{1,2})"""
        "pv.SingleDigitInteger" -> """(?:[0-9])"""
        else -> null
    }

    private fun lineToRegex(lineRaw: String, g: Grammar): String {
        var s = lineRaw.trim().replace("\\s+".toRegex(), " ")
        Log.d("EEUM_RuleCompiler", "[raw] $lineRaw")

        // 1) (옵션) — 슬롯/매크로 확장 전에 처리
        val optRe = Regex("""\(([^()\[\]]+?)\)""")
        s = optRe.replace(s) { m ->
            val content = m.groupValues[1].trim()
            val hasMacroOrSlot = content.contains('@') || content.contains('$')
            if (!hasMacroOrSlot && content.contains(",")) {
                val alts = content.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                "(?:" + alts.joinToString("|") { flexHangul(it) } + ")?"
            } else {
                // 매크로/슬롯/기타는 그대로 감싸기만
                "(?:$content)?"
            }
        }
        Log.d("EEUM_RuleCompiler", "[after option()] $s")

        // 2) [OR] — 슬롯/매크로 확장 전에 처리
        val orRe = Regex("""\[(.+?)\]""")
        s = orRe.replace(s) { m ->
            val alts = m.groupValues[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            "(?:" + alts.joinToString("|") { t ->
                if (t.startsWith("@") || t.startsWith("$")) t else flexHangul(t)
            } + ")"
        }
        Log.d("EEUM_RuleCompiler", "[after [or]] $s")

        // 3) @매크로 확장
        val macroRe = Regex("""@([A-Za-z0-9_가-힣]+)""")
        s = macroRe.replace(s) { m ->
            val name = m.groupValues[1]
            val list = g.context.macros[name] ?: emptyList()
            if (list.isEmpty()) reQuoteLiteral(m.value) else buildMacroPattern(name, list)
        }
        Log.d("EEUM_RuleCompiler", "[after @macro] $s")

        // 4) $슬롯 확장
        val slotRe = Regex("""\$([A-Za-z0-9_.가-힣]+):([A-Za-z0-9_]+)""")
        s = slotRe.replace(s) { m ->
            val slotLabel = m.groupValues[1]
            val slotKey   = m.groupValues[2]
            val vocabByLabel = g.context.slots[slotLabel]
            val vocabByKey   = g.context.slots[slotKey]
            val vocab = vocabByLabel ?: vocabByKey

            val inner = when {
                pvPatternOf(slotLabel) != null -> pvPatternOf(slotLabel)!!
                vocab != null && vocab.isNotEmpty() ->
                    "(?:" + vocab.joinToString("|") { flexHangul(it) } + ")"
                else -> """\S+"""
            }
            "(?<$slotKey>$inner)"
        }
        Log.d("EEUM_RuleCompiler", "[after slots] $s")

        // 5) 공백 유연화
        s = s.replace(" ", "\\s*")
        Log.d("EEUM_RuleCompiler", "[after spaces] $s")

        val final = ".*$s.*"

        // 6) 안전점검: 만약 \?: 또는 \|가 생기면 경고
        if (final.contains("""\(\?:\\\?:""".toRegex()) || final.contains("""\\\|""".toRegex())) {
            Log.w("EEUM_RuleCompiler", "[warn] Suspicious escapes in: $final")
        }

        Log.d("EEUM_RuleCompiler", "[final] $final")
        return final
    }

    private fun buildMacroPattern(name: String, list: List<String>): String {
        fun guardVerbStem(token: String): String {
            // 한글 토큰에 공백유연화까지 적용
            val flex = flexHangul(token)
            // 단독 어간일 때만 가드. (켜/꺼)
            return when (token) {
                "켜" -> """(?:$flex)(?!\s*(?:져|진|져\s*있|져\s*있는))"""
                "꺼" -> """(?:$flex)(?!\s*(?:져|진|져\s*있|져\s*있는))"""
                else -> flex
            }
        }

        val items = when (name) {
            "켜다", "끄다" -> list.map { guardVerbStem(it) }
            else           -> list.map { flexHangul(it) }
        }
        return "(?:" + items.joinToString("|") + ")"
    }
}