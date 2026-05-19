package com.alicejump.yandeviewer.sync

import android.content.Context
import com.alicejump.yandeviewer.network.RetrofitClient
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object TagSyncer {

    private val isSyncing = AtomicBoolean(false)
    // Batch writes to reduce I/O pressure while avoiding excessive in-memory accumulation.
    private const val BATCH_WRITE_THRESHOLD = 1500

    fun launchSync(context: Context) {
        if (isSyncing.getAndSet(true)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lastSavedId = TagTypeCache.getLastSyncedId(context)

                var firstNewId: Long? = null
                var page = 1
                var syncCompletedSuccessfully = false

                // 👉【新增】全量暂存区
                val totalNewTags = mutableMapOf<String, Int>()
                var foundAnyNewTags = false

                while (true) {
                    val tagsFromApi = try {
                        RetrofitClient.api.getTagsByPage(page = page)
                    } catch (_: Exception) {
                        break
                    }

                    if (tagsFromApi.isEmpty()) {
                        syncCompletedSuccessfully = true
                        break
                    }

                    var newTagsFoundInPage = false

                    tagsFromApi.forEach { tag ->

                        // ✅ 只处理真正的新标签
                        if (tag.id >= lastSavedId) {

                            newTagsFoundInPage = true
                            foundAnyNewTags = true

                            if (firstNewId == null) {
                                firstNewId = tag.id.toLong()
                            }

                            // ✅ 只把新标签放入
                            totalNewTags[tag.name] = tag.type
                        }
                    }

                    val maxIdInPage = tagsFromApi.maxOfOrNull { it.id.toLong() } ?: Long.MIN_VALUE
                    if (!newTagsFoundInPage && (page == 1 || maxIdInPage < lastSavedId)) {
                        syncCompletedSuccessfully = true
                        break
                    }

                    // Batch writes to avoid losing all fetched data on mid-sync failure.
                    if (totalNewTags.size >= BATCH_WRITE_THRESHOLD) {
                        TagTypeCache.addTags(context, totalNewTags)
                        totalNewTags.clear()
                    }

                    page++
                    delay(120)
                }

                if (totalNewTags.isNotEmpty()) {
                    TagTypeCache.addTags(context, totalNewTags)
                }
                // Advance last_id only after a full successful scan to avoid skipping data after interruptions.
                if (syncCompletedSuccessfully && foundAnyNewTags) {
                    firstNewId?.let { TagTypeCache.updateLastSyncedId(context, it) }
                }

            } finally {
                withContext(NonCancellable) {
                    TagTypeCache.flushNow(context)
                }
                isSyncing.set(false)
            }
        }
    }
}
