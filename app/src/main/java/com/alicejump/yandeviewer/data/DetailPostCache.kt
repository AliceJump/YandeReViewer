package com.alicejump.yandeviewer.data

import com.alicejump.yandeviewer.model.Post
import java.util.LinkedHashMap
import java.util.UUID

object DetailPostCache {
    private const val MAX_CACHE_ENTRIES = 8

    private val cache = LinkedHashMap<String, List<Post>>(MAX_CACHE_ENTRIES, 0.75f, true)

    @Synchronized
    fun put(posts: List<Post>): String {
        val key = UUID.randomUUID().toString()
        cache[key] = posts.toList()
        while (cache.size > MAX_CACHE_ENTRIES) {
            val oldestKey = cache.entries.firstOrNull()?.key ?: break
            cache.remove(oldestKey)
        }
        return key
    }

    @Synchronized
    fun get(key: String?): List<Post>? {
        if (key.isNullOrBlank()) return null
        return cache[key]
    }
}
