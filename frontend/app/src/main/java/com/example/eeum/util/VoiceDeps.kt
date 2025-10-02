package com.example.eeum.util

import com.example.eeum.base.DeviceDirectoryCache
import com.example.eeum.base.RoutineDirectoryCache

// 싱글톤
object VoiceDeps {
    @Volatile var directory: DeviceDirectoryCache? = null
    @Volatile var routineDirectory: RoutineDirectoryCache? = null
}