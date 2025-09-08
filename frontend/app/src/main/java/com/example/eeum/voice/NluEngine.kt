package com.example.eeum.voice

import android.util.Log
import com.example.eeum.data.model.dto.voice.CompiledIntent
import com.example.eeum.data.model.dto.voice.IntentResult

class NluEngine(
    private val intents: List<CompiledIntent>,
    connectors: List<String>
) {
    private val clauseSplit = Regex(
        "(?<=^|\\s)(?:" + connectors.joinToString("|") { Regex.escape(it) } + ")(?=\\s|$)"
    )

    fun parseUtterance(utterance: String): List<IntentResult> {
        // ── 0) Pre/regex
        val spaceNorm = "\\s+".toRegex()
        val text = utterance.trim().replace(spaceNorm, " ")

        // 상태+의문 감지 (QUERY 우선 판정용)
        val rxPowerState  = Regex("(켜\\s*져|켜\\s*진|꺼\\s*져|꺼\\s*진|켜\\s*져\\s*있|꺼\\s*져\\s*있)")
        val rxExistQ      = Regex("(있어|있니|있나요|있습니까|있냐)")
        val rxPowerStatus = Regex("전원\\s*(상태|상황)\\s*(알려줘|알려줘요|알려주세요|말해줘|말해줘요|알려주라|알려줄래|알려줄수있어|얼마|얼마나|얼마에요|어때|어때요|어떤가|어떤가요)")

        // ── 1) 절 단위 분리
        val clauses = text
            .split(clauseSplit)         // ex) "그리고/하고/그 다음에…" 등을 기준으로 분리
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        Log.d("NluEngine", "[text] $text")
        Log.d("NluEngine", "[clauses] ${clauses.joinToString(separator = " | ")}")

        // ── 2) 절-별 상속 슬롯 및 매칭
        val inherit = mutableMapOf<String, String>()
        // (clauseIdx, result)로 들고 있다가 후처리에서 절 기준 판정에 활용
        val rawPairs = mutableListOf<Pair<Int, IntentResult>>()

        clauses.forEachIndexed { idx, c ->
            val clauseText = c.replace(spaceNorm, " ")
            val r = parseOneClause(clauseText, inherit)

            if (r == null) {
                Log.d("NluEngine", "NO_MATCH@seg$idx text='$clauseText'")
                return@forEachIndexed
            }

            // 상속에 채우기
            r.slots.forEach { (k, v) -> if (v.isNotBlank()) inherit[k] = v }

            Log.d("NluEngine", "HIT@seg$idx ${r.intent} slots=${r.slots} seg='$clauseText'")
            rawPairs += idx to r
        }

        if (rawPairs.isEmpty()) {
            Log.d("NluEngine", "[results.final] (empty)")
            return emptyList()
        }

        // ── 3) 절 단위로 QUERY 우선 규칙 적용
        //  - 해당 절이 '상태+의문' 또는 '전원 상태 문의'로 보이면 TURN_ON/OFF는 버림
        val queryishSegs: Set<Int> = clauses.mapIndexedNotNull { i, c ->
            val t = c.replace(spaceNorm, " ")
            val isQueryish = (rxPowerState.containsMatchIn(t) && rxExistQ.containsMatchIn(t)) ||
                    rxPowerStatus.containsMatchIn(t)
            if (isQueryish) i else null
        }.toSet()

        val filteredPairs = rawPairs.filter { (segIdx, res) ->
            val isCommand = res.intent == "TURN_ON_DEVICE" || res.intent == "TURN_OFF_DEVICE"
            if (segIdx in queryishSegs && isCommand) {
                Log.d("NluEngine", "DROP@seg$segIdx ${res.intent} (query-priority)")
                false
            } else true
        }

        // ── 4) 같은 절 안에서 중복/충돌 정리(선택 규칙)
        //  - 슬롯 많이 채운 결과 우선
        //  - 같은 intent/주요 슬롯 조합은 1개만 유지
        val bySeg = filteredPairs.groupBy { it.first }.toSortedMap()
        val deduped = mutableListOf<IntentResult>()

        bySeg.forEach { (segIdx, list) ->
            // 같은 절에서 결과가 여러 개인 경우 슬롯 수 내림차순 → 고정 우선순위
            val sorted = list
                .sortedByDescending { (_, r) -> r.slots.count { it.value.isNotBlank() } }
                .map { it.second }

            // intent+주요 슬롯(device/place 등) 기준으로 중복 제거
            val seen = HashSet<String>()
            for (res in sorted) {
                val key = buildString {
                    append(res.intent)
                    append('|'); append(res.slots["device"] ?: "")
                    append('|'); append(res.slots["place"] ?: "")
                    append('|'); append(res.slots["name"] ?: "")
                    append('|'); append(res.slots["kinship"] ?: "")
                }
                if (seen.add(key)) {
                    deduped += res
                } else {
                    Log.d("NluEngine", "DEDUP@seg$segIdx ${res.intent} slots=${res.slots}")
                }
            }
        }

        Log.d(
            "NluEngine",
            "[results.final] " + deduped.joinToString { it.intent + ":" + it.slots.toString() }
        )
        return deduped
    }

    private fun parseOneClause(clause: String, inherit: Map<String,String>): IntentResult? {
        for (ci in intents) {
            for (rule in ci.rules) {
                val m = rule.regex.find(clause) ?: continue

                val slots = mutableMapOf<String,String>()
                // ★ 이 패턴(rule)에 실제로 존재하는 슬롯만 조회!
                for (name in rule.slotNames) {
                    val v = m.groups[name]?.value
                    if (!v.isNullOrBlank()) slots[name] = v
                    else inherit[name]?.let { slots[name] = it }
                }

                return IntentResult(ci.intent, slots.filterValues { it.isNotBlank() })
            }
        }
        return null
    }
}