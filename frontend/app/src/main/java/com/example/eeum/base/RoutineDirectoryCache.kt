package com.example.eeum.base

import com.example.eeum.data.model.response.common.Page
import com.example.eeum.data.model.response.routine.RoutineResponse
import com.example.eeum.data.remote.service.RoutineService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoutineDirectoryCache(
    private val routineService: RoutineService
) {
    @Volatile private var byName: Map<String, Int> = emptyMap()

    suspend fun loadAllRoutines(): Int = withContext(Dispatchers.IO) {
        val res = routineService.readRoutines()
        if (!res.isSuccessful) throw IllegalStateException("readRoutines HTTP ${res.code()}")
        val api = res.body() ?: throw IllegalStateException("readRoutines empty body")
        if (api.status != "SUCCESS") throw IllegalStateException("readRoutines status=${api.status}")

        val page: Page<RoutineResponse> = api.data
        val items = page.items

        val map = HashMap<String, Int>(items.size)
        for (r in items) {
            val key = normalize(r.name)
            if (key.isNotEmpty()) map[key] = r.routineId
        }
        byName = map
        items.size
    }

    fun findIdByName(name: String?): Int? {
        val key = normalize(name)
        return if (key.isEmpty()) null else byName[key]
    }

    fun upsert(name: String, id: Int) {
        val m = HashMap(byName)
        val key = normalize(name)
        if (key.isNotEmpty()) {
            m[key] = id
            byName = m
        }
    }

    private fun normalize(s: String?): String =
        s?.trim()?.replace("\\s+".toRegex(), " ")?.lowercase().orEmpty()
}