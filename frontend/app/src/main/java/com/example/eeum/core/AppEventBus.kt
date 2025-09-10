package com.example.eeum.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEventBus {

    private val _effects = MutableSharedFlow<AppEffect>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // 구독용 스트림
    val effects: SharedFlow<AppEffect> = _effects.asSharedFlow()

    // 일시 중단 가능한 emit
    suspend fun emit(effect: AppEffect) {
        _effects.emit(effect)
    }

    // 코루틴 컨텍스트 없이도 시도 가능한 비중단 emit
    fun tryEmit(effect: AppEffect): Boolean = _effects.tryEmit(effect)
}