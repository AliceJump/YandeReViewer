package com.alicejump.yandeviewer.viewmodel

import android.content.Context
import com.alicejump.yandeviewer.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object TagTypeCache {

    // Centralized file constants
    const val TAG_DICT_FILE = "tags_name_type.json"
    const val LAST_ID_FILE = "last_id.txt"

    private val _tagTypes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tagTypes = _tagTypes.asStateFlow()
    private val initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private val mutex = Mutex() // To protect file and cache access

    private val pending = mutableMapOf<String, Int>()
    private var lastWriteTime = 0L
    private const val WRITE_INTERVAL = 800L   // 0.8秒合并窗口

    fun initialize(context: Context) {
        if (initialized.getAndSet(true)) return

        scope.launch {
            ensureAssetCopied(context)
            loadFromFile(context)
        }
    }

    private suspend fun ensureAssetCopied(context: Context) {
        withContext(Dispatchers.IO) {
            val targetTagFile = File(context.filesDir, TAG_DICT_FILE)
            val targetIdFile = File(context.filesDir, LAST_ID_FILE)

            // 如果已经存在，说明不是首次启动
            if (targetTagFile.exists() && targetIdFile.exists()) {
                return@withContext
            }

            try {
                context.assets.open(TAG_DICT_FILE).use { input ->
                    targetTagFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                context.assets.open(LAST_ID_FILE).use { input ->
                    targetIdFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadFromFile(context: Context) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val tagDictFile = File(context.filesDir, TAG_DICT_FILE)
                if (tagDictFile.exists()) {
                    try {
                        val type = object : TypeToken<Map<String, Int>>() {}.type
                        gson.fromJson<Map<String, Int>>(tagDictFile.reader(), type)?.let {
                            _tagTypes.value = it
                        }
                    } catch (e: Exception) {
                        // Handle possible json parsing errors by starting fresh
                        _tagTypes.value = emptyMap()
                    }
                }
            }
        }
    }

    suspend fun getLastSyncedId(context: Context): Long {
        return withContext(Dispatchers.IO) {
            File(context.filesDir, LAST_ID_FILE).run {
                if (exists()) readText().toLongOrNull() ?: 0L else 0L
            }
        }
    }

    suspend fun updateLastSyncedId(context: Context, newId: Long) {
        withContext(Dispatchers.IO) {
            File(context.filesDir, LAST_ID_FILE).writeText(newId.toString())
        }
    }

    fun flush(context: Context) {
        scope.launch {
            mutex.withLock {
                if (pending.isEmpty()) return@launch

                val merged = _tagTypes.value + pending
                _tagTypes.value = merged

                persistToFile(context, merged)

                pending.clear()
                lastWriteTime = System.currentTimeMillis()
            }
        }
    }

    suspend fun addTags(context: Context, newTags: Map<String, Int>) {
        if (newTags.isEmpty()) return

        mutex.withLock {
            // 1. 先进缓冲
            pending.putAll(newTags)

            val now = System.currentTimeMillis()

            // 2. 未到时间 → 只更新内存
            if (now - lastWriteTime < WRITE_INTERVAL) {
                _tagTypes.value += newTags
                return
            }

            // 3. 到时间 → 真正合并写盘
            val merged = _tagTypes.value + pending
            _tagTypes.value = merged

            persistToFile(context, merged)

            pending.clear()
            lastWriteTime = now
        }
    }


    fun prioritizeTags(context: Context, tags: Set<String>) {
        val tagsToFetch = tags.filter { !_tagTypes.value.containsKey(it) }
        if (tagsToFetch.isEmpty()) return

        scope.launch {
            // Fetch missing tags one by one for simplicity.
            // In a production app, you might want to batch these network requests.
            tagsToFetch.forEach { tag ->
                fetchAndSaveTag(context, tag)
            }
        }
    }

    private suspend fun fetchAndSaveTag(context: Context, tag: String) {
        val newTagType = try {
            RetrofitClient.api.getTags(tag).find { it.name == tag }?.type ?: 0
        } catch (_: Exception) {
            0 // Default on error
        }
        // Use the centralized addTags function to ensure thread safety
        addTags(context, mapOf(tag to newTagType))
    }

    private suspend fun persistToFile(context: Context, tags: Map<String, Int>) {
        withContext(Dispatchers.IO) {
            try {
                File(context.filesDir, TAG_DICT_FILE).writeText(gson.toJson(tags))
            } catch (e: Exception) {
                // Consider logging the error
            }
        }
    }
}
