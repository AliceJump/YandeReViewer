package com.alicejump.yandeviewer.data

import com.alicejump.yandeviewer.model.Post
import java.util.LinkedHashMap
import java.util.UUID

object DetailPostCache {
    private const val MAX_CACHE_ENTRIES = 8

    private val cache = object : LinkedHashMap<String, List<Post>>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Post>>): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    @Synchronized
    fun put(posts: List<Post>): String {
        val key = UUID.randomUUID().toString()
        cache[key] = posts.toList()
        return key
    }

    @Synchronized
    fun get(key: String?): List<Post>? {
        if (key.isNullOrBlank()) return null
        return cache[key]
    }
}
