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
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

// 下载图片方法
fun downloadImage(context: Context, post: Post, showToast: Boolean = true) {
    val appFolder = context.getString(R.string.app_name)
    val extension = post.file_url.substringAfterLast('.', "jpg").lowercase()
    val fileName = "yande.re_${post.id}.$extension".replace("[^a-zA-Z0-9._-]".toRegex(), "_")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (showToast) {
            Toast.makeText(context, "Started downloading", Toast.LENGTH_SHORT).show()
        }
        // Android 10 及以上使用 MediaStore API
        CoroutineScope(Dispatchers.IO).launch {

            var uri: Uri? = null   // ⭐新增

            try {

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

                uri = resolver.insert(   // ⭐修改
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )

                if (uri != null) {

                    resolver.openOutputStream(uri)?.use { output ->
                        URL(post.file_url).openStream().use { input ->
                            input.copyTo(output)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (showToast) {
                            Toast.makeText(context, "Download completed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: Exception) {

                uri?.let {   // ⭐新增：失败删除文件
                    try {
                        context.contentResolver.delete(it, null, null)
                    } catch (_: Exception) {}
                }

                withContext(Dispatchers.Main) {
                    if (showToast) {
                        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

    } else {
        // Android 10 以下使用 DownloadManager
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(post.file_url.toUri())
            .setTitle("Downloading Post ${post.id}")
            .setDescription(post.tags)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "$appFolder/$fileName"
            )
        downloadManager.enqueue(request)
        Toast.makeText(context, "Started downloading", Toast.LENGTH_SHORT).show()
    }
}