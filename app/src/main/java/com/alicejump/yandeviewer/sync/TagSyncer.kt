package com.alicejump.yandeviewer.sync

import android.content.Context
import com.alicejump.yandeviewer.network.RetrofitClient
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object TagSyncer {

    private val isSyncing = AtomicBoolean(false)

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

                            if (firstNewId == null) {
                                firstNewId = tag.id.toLong()
                            }

                            // ✅ 只把新标签放入
                            totalNewTags[tag.name] = tag.type
                        }
                    }

                    if (page == 1 && !newTagsFoundInPage) {
                        syncCompletedSuccessfully = true
                        break
                    }

                    page++
                    delay(120)
                }

                // 👉【关键】只在最后一次写
                if (syncCompletedSuccessfully && totalNewTags.isNotEmpty()) {

                    TagTypeCache.addTags(context, totalNewTags)

                    firstNewId?.let {
                        TagTypeCache.updateLastSyncedId(context, it)
                    }
                }

            } finally {
                TagTypeCache.flush(context)   // ← 新增这行
                isSyncing.set(false)
            }
        }
    }
}
