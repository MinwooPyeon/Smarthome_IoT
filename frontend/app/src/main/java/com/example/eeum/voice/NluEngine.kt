package com.example.eeum.voice

import android.util.Log
import com.example.eeum.data.model.dto.voice.CompiledIntent
import com.example.eeum.data.model.dto.voice.IntentResult

private const val TAG = "EEUM_NluEngine"

class NluEngine(
    private val intents: List<CompiledIntent>,
    connectors: List<String>
) {
    private val splitRegex = Regex(
        "(?<=^|\\s)(?:" +
                (listOf("\\|") + connectors.map { Regex.escape(it) }).joinToString("|") +
                ")(?=\\s|$)"
    )

    private fun normalizeForClauseSplit(t: String): String {
        return t
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(켜|끄|설정|맞춰|바꿔|틀어|연결)\\s*고"), "$1 |")
            .replace(Regex("(켜|끄|설정|맞춰|바꿔|틀어|연결)\\s*주고"), "$1 |")
            .replace(Regex("\\b(그리고|그\\s*다음에|그다음에|그리고나서|하고|한\\s*다음에|다음에)\\b"), "|")
            .replace(Regex("\\s*\\|+\\s*"), " | ")
            .trim()
    }

    fun parseUtterance(utterance: String): List<IntentResult> {
        val spaceNorm = "\\s+".toRegex()

        val rxNumMention   = Regex("\\b(\\d{1,2})\\s*번\\b")
        val rxDeviceMention= Regex("\\b(에어컨|선풍기|전등|프로젝터|빔프로젝터|빔|조명|불|냉방기|팬)\\b")
        val rxPlaceMention = Regex("\\b(방|거실|주방|부엌|침실|베란다|발코니|서재|작업실|화장실|홀)\\b")

        // 0) 공백 정규화
        val text0 = utterance.trim().replace(spaceNorm, " ")

        // 1) 절 분리(동사+고/주고/그리고/그다음에 → '|')
        val text = normalizeForClauseSplit(text0)
        val clauses = text
            .split(splitRegex)           // 파이프(|) + 주입된 connectors 모두 split 대상으로
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        Log.d(TAG, "[text] $text")
        Log.d(TAG, "[clauses] ${clauses.joinToString(" | ")}")

        // 2) 절-별 매칭 + 컨텍스트 상속(화이트리스트)
        val CONTEXT_KEYS = setOf("place", "device", "num")   // 상속 유지할 컨텍스트 키만
        val inherit = mutableMapOf<String, String>()
        val rawPairs = mutableListOf<Pair<Int, IntentResult>>()  // (segIdx, mergedResult)

        clauses.forEachIndexed { idx, c ->
            val clauseText = c.replace(spaceNorm, " ")
            val r = parseOneClause(clauseText, inherit)

            if (r == null) {
                Log.d(TAG, "NO_MATCH@seg$idx text='$clauseText'")
                return@forEachIndexed
            }

            Log.d(TAG, "RAW@seg$idx intent=${r.intent} rawSlots=${r.slots} text='$clauseText'")

            // ★ 관측 로그 ②: 트리거 감지(숫자+번/기기/장소 언급)
            val trgNum    = rxNumMention.containsMatchIn(clauseText)
            val trgDevice = rxDeviceMention.containsMatchIn(clauseText)
            val trgPlace  = rxPlaceMention.containsMatchIn(clauseText)
            Log.d(TAG, "TRG@seg$idx num=$trgNum device=$trgDevice place=$trgPlace")

            // 상속 베이스 = 컨텍스트 키만 유지
            val base = inherit.filterKeys { it in CONTEXT_KEYS }.toMutableMap()

            // 현재 절 슬롯으로 override (현재 절 우선)
            r.slots.forEach { (k, v) -> if (v.isNotBlank()) base[k] = v }

            // 장소가 새로 말해졌는데 num이 없으면 num 리셋(방 번호는 장소 종속)
            if ("place" in r.slots && "num" !in r.slots) {
                base.remove("num")
            }

            if (trgNum && "num" !in r.slots && base.remove("num") != null) {
                Log.d(TAG, "GUARD@seg$idx drop inherited num (num-mention without slot)")
            }

            if (trgDevice && "device" !in r.slots && base.remove("device") != null) {
                Log.d(TAG, "GUARD@seg$idx drop inherited device (device-mention without slot)")
            }

            // 다음 절을 위한 상속 업데이트
            inherit.clear(); inherit.putAll(base)

            // 결과에도 '완성된 슬롯' 저장
            val out = IntentResult(r.intent, base.toMutableMap())

            Log.d(TAG, "HIT@seg$idx ${out.intent} slots=${out.slots} seg='$clauseText'")
            rawPairs += idx to out
        }

        if (rawPairs.isEmpty()) {
            Log.d(TAG, "[results.final] (empty)")
            return emptyList()
        }

        // 3) 해당 절이 질의 성격이면(전원 상태 + 의문/요청) TURN_* 명령은 드랍
        val rxPowerState  = Regex("(켜\\s*져|켜\\s*진|꺼\\s*져|꺼\\s*진|켜\\s*져\\s*있|꺼\\s*져\\s*있)")
        val rxExistQ      = Regex("(있어|있니|있나요|있습니까|있냐)")
        val rxPowerStatus = Regex("전원\\s*(상태|상황)\\s*(알려줘|알려줘요|알려주세요|말해줘|말해줘요|알려주라|알려줄래|알려줄수있어|얼마|얼마나|얼마에요|어때|어때요|어떤가|어떤가요)")

        val queryishSegs: Set<Int> = clauses.mapIndexedNotNull { i, c ->
            val t = c.replace(spaceNorm, " ")
            val isQueryish = (rxPowerState.containsMatchIn(t) && rxExistQ.containsMatchIn(t)) ||
                    rxPowerStatus.containsMatchIn(t)
            if (isQueryish) i else null
        }.toSet()

        val filteredPairs = rawPairs.filter { (segIdx, res) ->
            val isCommand = res.intent == "TURN_ON_DEVICE" || res.intent == "TURN_OFF_DEVICE"
            if (segIdx in queryishSegs && isCommand) {
                Log.d(TAG, "DROP@seg$segIdx ${res.intent} (query-priority)")
                false
            } else true
        }

        // 4) 같은 절 안에서 중복/충돌 정리: 슬롯 많이 채운 결과 우선, intent+핵심 슬롯 기준 dedupe
        val bySeg = filteredPairs.groupBy { it.first }.toSortedMap()
        val deduped = mutableListOf<IntentResult>()

        bySeg.forEach { (segIdx, list) ->
            val sorted = list
                .sortedByDescending { (_, r) -> r.slots.count { it.value.isNotBlank() } }
                .map { it.second }

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
                    Log.d(TAG, "DEDUP@seg$segIdx ${res.intent} slots=${res.slots}")
                }
            }
        }

        Log.d(TAG, "[results.final] " + deduped.joinToString { it.intent + ":" + it.slots.toString() })
        return deduped
    }

    private fun parseOneClause(clause: String, inherit: Map<String,String>): IntentResult? {
        var best: IntentResult? = null
        var bestExplicit = -1
        var ruleOrder = 0
        var bestOrder = Int.MAX_VALUE

        for (ci in intents) {
            for (rule in ci.rules) {
                val m = rule.regex.find(clause)
                if (m == null) {
                    ruleOrder++
                    continue
                }

                val slots = mutableMapOf<String,String>()
                var explicit = 0
                for (name in rule.slotNames) {
                    val v = m.groups[name]?.value
                    if (!v.isNullOrBlank()) {
                        slots[name] = v
                        explicit++
                    }
                }

                if (explicit > bestExplicit || (explicit == bestExplicit && ruleOrder < bestOrder)) {
                    best = IntentResult(ci.intent, slots)
                    bestExplicit = explicit
                    bestOrder = ruleOrder
                }
                ruleOrder++
            }
        }
        return best
    }
}