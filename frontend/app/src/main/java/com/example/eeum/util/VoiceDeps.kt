package com.example.eeum.util

import com.example.eeum.base.DeviceDirectoryCache

// 싱글톤
object VoiceDeps {
    @Volatile var directory: DeviceDirectoryCache? = null
}