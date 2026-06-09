package com.alicejump.yandeviewer.sync

import android.content.Context
import android.util.Log
import com.alicejump.yandeviewer.network.RetrofitClient
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "TagSyncer"

object TagSyncer {

    private val isSyncing = AtomicBoolean(false)
    // Batch writes to reduce I/O pressure while avoiding excessive in-memory accumulation.
    private const val BATCH_WRITE_THRESHOLD = 1500

    fun launchSync(context: Context) {
        if (isSyncing.getAndSet(true)) {
            Log.d(TAG, "Already syncing, skip launch")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lastSavedId = TagTypeCache.getLastSyncedId(context)
                Log.d(TAG, "🔄 TagSync started, lastSavedId=$lastSavedId")

                var lastNewId: Long = lastSavedId  // ← 改为记录最后的新ID
                var page = 1
                var syncCompletedSuccessfully = false

                // 👉【新增】全量暂存区
                val totalNewTags = mutableMapOf<String, Int>()
                var foundAnyNewTags = false

                while (true) {
                    val tagsFromApi = try {
                        RetrofitClient.api.getTagsByPage(page = page)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ API error on page $page", e)
                        break
                    }

                    if (tagsFromApi.isEmpty()) {
                        Log.d(TAG, "✅ API returned empty on page $page, sync completed")
                        syncCompletedSuccessfully = true
                        break
                    }

                    Log.d(TAG, "📄 Page $page: ${tagsFromApi.size} tags from API")

                    var newTagsFoundInPage = false

                    tagsFromApi.forEach { tag ->

                        // ✅ 只处理真正的新标签
                        if (tag.id > lastSavedId) {  // ← 改为 > 而非 >=

                            newTagsFoundInPage = true
                            foundAnyNewTags = true
                            lastNewId = tag.id.toLong()  // 持续更新最新的ID

                            // ✅ 只把新标签放入
                            totalNewTags[tag.name] = tag.type
                        }
                    }

                    Log.d(TAG, "   New tags in this page: ${totalNewTags.size} accumulated")

                    val maxIdInPage = tagsFromApi.maxOfOrNull { it.id.toLong() }
                    if (!newTagsFoundInPage && (page == 1 || (maxIdInPage != null && maxIdInPage < lastSavedId))) {
                        Log.d(TAG, "✅ No new tags found and maxId < lastSavedId, sync completed")
                        syncCompletedSuccessfully = true
                        break
                    }

                    // Batch writes to avoid losing all fetched data on mid-sync failure.
                    if (totalNewTags.size >= BATCH_WRITE_THRESHOLD) {
                        Log.d(TAG, "💾 Batch write: ${totalNewTags.size} tags")
                        TagTypeCache.addTags(context, totalNewTags)
                        totalNewTags.clear()
                    }

                    page++
                    delay(120)
                }

                if (totalNewTags.isNotEmpty()) {
                    Log.d(TAG, "💾 Final batch write: ${totalNewTags.size} tags")
                    TagTypeCache.addTags(context, totalNewTags)
                }
                // Advance last_id only after a full successful scan to avoid skipping data after interruptions.
                if (syncCompletedSuccessfully && foundAnyNewTags && lastNewId > lastSavedId) {
                    Log.d(TAG, "📝 Update lastSyncedId from $lastSavedId to $lastNewId")
                    TagTypeCache.updateLastSyncedId(context, lastNewId)
                }

                Log.d(TAG, "✅ Sync logic complete, flushing...")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in sync", e)
            } finally {
                withContext(NonCancellable) {
                    Log.d(TAG, "🔒 Final flush...")
                    TagTypeCache.flushNow(context)
                }
                Log.d(TAG, "🏁 TagSync finished")
                isSyncing.set(false)
            }
        }
    }
}
