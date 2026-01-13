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
import kotlinx.coroutines.withContext
import java.io.File

object TagTypeCache {

    private const val TAG_DICT_FILE = "tags_name_type.json"

    private val _tagTypes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tagTypes = _tagTypes.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    fun initialize(context: Context) {
        scope.launch {
            loadFromFile(context)
        }
    }

    private suspend fun loadFromFile(context: Context) {
        withContext(Dispatchers.IO) {
            val tagDictFile = File(context.filesDir, TAG_DICT_FILE)
            if (tagDictFile.exists()) {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                val tags: Map<String, Int> = gson.fromJson(tagDictFile.reader(), type)
                _tagTypes.value = tags
            }
        }
    }

    fun getTagType(context: Context, tag: String) {
        // If tag is not in memory, fetch from network
        if (!_tagTypes.value.containsKey(tag)) {
            scope.launch {
                fetchAndSaveTag(context, tag)
            }
        }
    }

    fun prioritizeTags(context: Context, tags: Set<String>) {
        tags.forEach { tag ->
            getTagType(context, tag)
        }
    }

    private suspend fun fetchAndSaveTag(context: Context, tag: String) {
        val newTagType = try {
            RetrofitClient.api.getTags(tag).find { it.name == tag }?.type ?: 0
        } catch (e: Exception) {
            0 // Default on error
        }

        // Update in-memory cache
        val updatedMap = _tagTypes.value + mapOf(tag to newTagType)
        _tagTypes.value = updatedMap

        // Persist to file
        withContext(Dispatchers.IO) {
            val tagDictFile = File(context.filesDir, TAG_DICT_FILE)
            tagDictFile.writeText(gson.toJson(updatedMap))
        }
    }
}
