package com.alicejump.yandeviewer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.alicejump.yandeviewer.network.ParallelImageFetcher
import com.alicejump.yandeviewer.sync.TagSyncer
import com.alicejump.yandeviewer.utils.CacheManager
import com.alicejump.yandeviewer.viewmodel.TagTypeCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MyApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // On first run, copy the pre-packaged data files from assets.
        copyInitialDataFiles()

        // Clear cache on start
        CacheManager.clearCacheOnStart(this)

        // Initialize the tag cache from local file
        TagTypeCache.initialize(this)
        // Launch the tag synchronization task in the background
        TagSyncer.launchSync(this)
    }

    private fun copyInitialDataFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            // List of files to copy from assets on first run
            val filesToCopy = listOf("tags_name_type.json", "last_id.txt")

            filesToCopy.forEach { fileName ->
                val destFile = File(filesDir, fileName)
                if (!destFile.exists()) {
                    try {
                        assets.open(fileName).use { inputStream ->
                            FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } catch (_: Exception) {
                        // File might not exist in assets, which is fine.
                        // e.g., the user doesn't want to provide a pre-set last_id.txt
                    }
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(ParallelImageFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    // Set the max size to 25% of the app's available memory.
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                CacheManager.newDiskCache(this)
            }
            .build()
    }
}
