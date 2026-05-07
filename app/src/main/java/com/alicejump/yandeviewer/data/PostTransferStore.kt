package com.alicejump.yandeviewer.data

import com.alicejump.yandeviewer.model.Post
import java.util.UUID

object PostTransferStore {
    const val EXTRA_POSTS_TRANSFER_KEY = "posts_transfer_key"
    private const val MAX_ENTRIES = 8

    private val cache = LinkedHashMap<String, List<Post>>(MAX_ENTRIES, 0.75f, true)
    private val lock = Any()

    fun put(posts: List<Post>): String {
        val key = UUID.randomUUID().toString()
        synchronized(lock) {
            // Defensive copy to avoid later mutation of mutable list implementations.
            cache[key] = posts.toList()
            trimToMaxSize()
        }
        return key
    }

    fun get(key: String?): List<Post>? {
        if (key.isNullOrBlank()) return null
        return synchronized(lock) { cache[key] }
    }

    fun remove(key: String?) {
        if (key.isNullOrBlank()) return
        synchronized(lock) { cache.remove(key) }
    }

    private fun trimToMaxSize() {
        while (cache.size > MAX_ENTRIES) {
            val oldestKey = cache.entries.firstOrNull()?.key ?: break
            cache.remove(oldestKey)
        }
    }
}
