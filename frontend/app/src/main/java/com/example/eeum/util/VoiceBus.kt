package com.example.eeum.util

import com.example.eeum.data.model.dto.voice.NluUpdate
import kotlinx.coroutines.flow.MutableSharedFlow

object VoiceBus {
    val updates = MutableSharedFlow<NluUpdate>(replay = 1)
}