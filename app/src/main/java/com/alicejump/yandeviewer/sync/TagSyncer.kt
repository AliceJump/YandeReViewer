package com.alicejump.yandeviewer.sync

import android.content.Context
import com.alicejump.yandeviewer.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object TagSyncer {

    private const val TAG_DICT_FILE = "tags_name_type.json"
    private const val LAST_ID_FILE = "last_id.txt"
    private val isSyncing = AtomicBoolean(false)

    fun launchSync(context: Context) {
        // Prevent multiple syncs from running at the same time
        if (isSyncing.getAndSet(true)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var syncCompletedSuccessfully = false
            try {
                val filesDir = context.filesDir
                val gson = Gson()

                // Step 1: Read existing data
                val tagDictFile = File(filesDir, TAG_DICT_FILE)
                val tagDict: MutableMap<String, Int> = if (tagDictFile.exists()) {
                    val type = object : TypeToken<MutableMap<String, Int>>() {}.type
                    gson.fromJson(tagDictFile.reader(), type)
                } else {
                    mutableMapOf()
                }

                val lastIdFile = File(filesDir, LAST_ID_FILE)
                val lastSavedId = if (lastIdFile.exists()) lastIdFile.readText().toLongOrNull() ?: 0L else 0L

                var firstNewId: Long? = null
                var page = 1

                // Step 2: Paginated fetch loop
                while (true) {
                    val tags = try {
                        RetrofitClient.api.getTagsByPage(page = page)
                    } catch (_: Exception) {
                        // Network error or other issues, break without marking as successful
                        break
                    }

                    if (tags.isEmpty()) {
                        syncCompletedSuccessfully = true // Normal termination
                        break
                    }

                    var newTagsFoundInPage = false
                    tags.forEach { tag ->
                        if (tag.id >= lastSavedId) {
                            newTagsFoundInPage = true
                            // Record the first new ID we encounter
                            if (firstNewId == null) {
                                firstNewId = tag.id.toLong()
                            }
                        }
                        tagDict[tag.name] = tag.type
                    }

                    // Save progress after each page
                    tagDictFile.writeText(gson.toJson(tagDict))

                    // If the first page contains no new tags, we can stop early.
                    if (page == 1 && !newTagsFoundInPage) {
                        syncCompletedSuccessfully = true // Normal termination
                        break
                    }

                    page++
                    delay(100) // Prevent rate-limiting
                }

                // Step 3: Save the new last_id ONLY if the sync completed without errors
                if (syncCompletedSuccessfully) {
                    firstNewId?.let {
                        lastIdFile.writeText(it.toString())
                    }
                }

            } finally {
                isSyncing.set(false)
            }
        }
    }
}
