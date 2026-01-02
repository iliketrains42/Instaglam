package com.example.instalgam.cache

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object ExoPlayerCache {
    @androidx.media3.common.util.UnstableApi
    private var cache: SimpleCache? = null

    @androidx.media3.common.util.UnstableApi
    fun get(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: buildCache(context.applicationContext).also {
                cache = it
            }
        }

    @androidx.media3.common.util.UnstableApi
    private fun buildCache(context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media")
        val evictor = LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024) // 200 MB
        val databaseProvider = StandaloneDatabaseProvider(context)

        return SimpleCache(cacheDir, evictor, databaseProvider)
    }
}
