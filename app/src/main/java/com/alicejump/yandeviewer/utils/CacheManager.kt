
package com.alicejump.yandeviewer.utils

import android.content.Context
import coil.disk.DiskCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object CacheManager {

    private const val MAX_SIZE = (250 * 1024 * 1024).toLong() // 250MB
    private const val HIGH_RES_THRESHOLD_BYTES = 1 * 1024 * 1024 // 1MB
    private const val CACHE_DIR_NAME = "image_cache"
    private const val KEEP_HIGH_RES_COUNT = 10
    private const val KEEP_PREVIEW_COUNT = 40

    fun newDiskCache(context: Context): DiskCache {
        return DiskCache.Builder()
            .directory(context.cacheDir.resolve(CACHE_DIR_NAME))
            .maxSizeBytes(MAX_SIZE)
            .build()
    }

    fun clearCacheOnStart(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val cacheDir = context.cacheDir.resolve(CACHE_DIR_NAME)
            if (!cacheDir.exists()) return@launch

            val allFiles = cacheDir.walk().filter { it.isFile }.toList()

            // Separate and sort high-resolution files
            val highResFiles = allFiles
                .filter { it.length() > HIGH_RES_THRESHOLD_BYTES }
                .sortedByDescending { it.lastModified() }

            // Separate and sort preview files
            val previewFiles = allFiles
                .filter { it.length() <= HIGH_RES_THRESHOLD_BYTES }
                .sortedByDescending { it.lastModified() }

            // Delete excess high-resolution files
            if (highResFiles.size > KEEP_HIGH_RES_COUNT) {
                highResFiles.drop(KEEP_HIGH_RES_COUNT).forEach { it.delete() }
            }

            // Delete excess preview files
            if (previewFiles.size > KEEP_PREVIEW_COUNT) {
                previewFiles.drop(KEEP_PREVIEW_COUNT).forEach { it.delete() }
            }
        }
    }
}
