package com.alicejump.yandeviewer.viewmodel

import com.alicejump.yandeviewer.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object TagTypeCache {
    private val _tagTypes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tagTypes = _tagTypes.asStateFlow()

    private val yandeApi = RetrofitClient.api
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val isDetailViewActive = AtomicBoolean(false)
    private val backgroundQueue = mutableSetOf<String>()

    /**
     * Called from DetailActivity to fetch tags for the current post with high priority.
     * It pauses background processing.
     */
    fun prioritizeTags(tags: Set<String>) {
        isDetailViewActive.set(true)
        scope.launch {
            fetchTags(tags)
        }
    }

    /**
     * Called from DetailActivity when it's destroyed.
     * Resumes background processing.
     */
    fun detailViewClosed() {
        isDetailViewActive.set(false)
        processBackgroundQueue() // Resume background work
    }

    /**
     * Called from the PagingSource to add tags for low-priority background fetching.
     */
    fun queueTagsForBackgroundFetching(tags: Set<String>) {
        scope.launch {
            val currentTypes = _tagTypes.value
            val newTags = tags.filter { !currentTypes.containsKey(it) && !backgroundQueue.contains(it) }
            if (newTags.isNotEmpty()) {
                backgroundQueue.addAll(newTags)
                processBackgroundQueue()
            }
        }
    }

    private fun processBackgroundQueue() {
        // Only process if detail view is not active
        if (isDetailViewActive.get()) return

        scope.launch {
            if (backgroundQueue.isNotEmpty()) {
                val tagToFetch = backgroundQueue.first()
                backgroundQueue.remove(tagToFetch)
                fetchTags(setOf(tagToFetch))
                // After fetching one tag, recursively call to process the next one
                processBackgroundQueue()
            }
        }
    }

    private suspend fun fetchTags(tags: Set<String>) {
        val currentTypes = _tagTypes.value
        val tagsToFetch = tags.filter { !currentTypes.containsKey(it) }

        if (tagsToFetch.isEmpty()) return

        val newTagTypes = tagsToFetch.associateWith { tag ->
            try {
                // Find the exact match from the returned list before getting the type.
                yandeApi.getTags(tag).find { it.name == tag }?.type ?: 0
            } catch (e: Exception) {
                0 // Default on error
            }
        }

        if (newTagTypes.isNotEmpty()) {
            _tagTypes.value = currentTypes + newTagTypes
        }
    }
}
