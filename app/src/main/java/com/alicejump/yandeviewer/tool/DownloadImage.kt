package com.alicejump.yandeviewer.tool

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.net.toUri
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL

// 下载图片方法 - 优先使用 Coil 缓存，如果不存在则等待缓存加载完成
@OptIn(ExperimentalCoilApi::class)
fun downloadImage(context: Context, post: Post, showToast: Boolean = true, cacheTimeoutMs: Long = 15000) {
    val appFolder = context.getString(R.string.app_name)
    val extension = post.file_url.substringAfterLast('.', "jpg").lowercase()
    val fileName = "yande.re_${post.id}.$extension".replace("[^a-zA-Z0-9._-]".toRegex(), "_")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (showToast) {
            Toast.makeText(context, R.string.started_downloading, Toast.LENGTH_SHORT).show()
        }
        // Android 10 及以上使用 MediaStore API
        CoroutineScope(Dispatchers.IO).launch {

            var uri: Uri? = null

            try {
                val imageLoader = context.imageLoader
                val diskCache = imageLoader.diskCache

                // ⭐ 首先尝试从缓存获取
                var snapshot = diskCache?.openSnapshot(post.file_url)

                // ⭐ 如果缓存不存在，等待缓存加载完成（带超时）
                if (snapshot == null) {
                    withTimeoutOrNull(cacheTimeoutMs) {
                        // 定期检查缓存是否存在，最多等待指定时间
                        val checkIntervalMs = 500L
                        var elapsedMs = 0L

                        while (elapsedMs < cacheTimeoutMs) {
                            kotlinx.coroutines.delay(checkIntervalMs)
                            snapshot = diskCache?.openSnapshot(post.file_url)
                            if (snapshot != null) {
                                break
                            }
                            elapsedMs += checkIntervalMs
                        }
                    }
                }

                val resolver = context.contentResolver

                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(
                        MediaStore.Downloads.MIME_TYPE, when (extension) {
                        "png" -> "image/png"
                        "gif" -> "image/gif"
                        else -> "image/jpeg"
                    })
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/$appFolder")
                }

                uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        // ⭐ 如果有缓存，使用缓存文件；否则从网络下载
                        if (snapshot != null) {
                            // 从缓存文件复制
                            snapshot!!.data.toFile().inputStream().use { input ->
                                input.copyTo(output)
                            }
                            snapshot!!.close()
                        } else {
                            // 超时或缓存加载失败，从网络下载（会自动缓存）
                            URL(post.file_url).openStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (showToast) {
                            Toast.makeText(context, R.string.download_completed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: Exception) {

                uri?.let {
                    try {
                        context.contentResolver.delete(it, null, null)
                    } catch (_: Exception) {}
                }

                withContext(Dispatchers.Main) {
                    if (showToast) {
                        Toast.makeText(context, context.getString(R.string.download_failed, e.message), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

    } else {
        // Android 10 以下使用 DownloadManager
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(post.file_url.toUri())
            .setTitle(context.getString(R.string.downloading_post, post.id))
            .setDescription(post.tags)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "$appFolder/$fileName"
            )
        downloadManager.enqueue(request)
        Toast.makeText(context, R.string.started_downloading, Toast.LENGTH_SHORT).show()
    }
}