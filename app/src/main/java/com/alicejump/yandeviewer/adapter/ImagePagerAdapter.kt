package com.alicejump.yandeviewer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.load
import com.alicejump.yandeviewer.PhotoViewActivity
import com.alicejump.yandeviewer.R
import com.alicejump.yandeviewer.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.abs

// RecyclerView 的适配器，用于显示图片列表，可分页浏览
class ImagePagerAdapter(private val posts: List<Post>) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_pager, parent, false)
        return ImageViewHolder(view)
    }

    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    // 返回列表数量
    override fun getItemCount(): Int = posts.size

    // ViewHolder 类，负责单个图片项的显示和交互
    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.pagerImageView)

        @SuppressLint("ClickableViewAccessibility")
        fun bind(post: Post) {
            // 设置图片共享元素过渡名称，用于 Activity 转场动画
            val transitionName = "image_transition_${post.id}"
            imageView.transitionName = transitionName

            // 使用 Coil 加载图片
            imageView.load(post.file_url) {
                allowHardware(false) // 禁用硬件加速以避免某些设备闪退
                placeholderMemoryCacheKey(post.preview_url) // 占位图使用预览图
                error(android.R.drawable.ic_menu_close_clear_cancel) // 加载失败显示默认错误图标
                crossfade(true) // 渐变效果
            }

            // 手势拖拽、长按与点击逻辑
            val handler = Handler(Looper.getMainLooper())
            var isDragging = false
            var downTime = 0L
            var startX = 0f
            var startY = 0f
            val dragTriggerDelayMillis = 1000L // 触发拖拽的延迟时间（1秒）

            // Runnable，用于延迟触发拖拽
            val dragRunnable = Runnable {
                isDragging = true
                startDrag(imageView, post)
            }

            // 设置触摸监听
            imageView.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录按下时间和初始坐标
                        downTime = System.currentTimeMillis()
                        startX = event.x
                        startY = event.y
                        isDragging = false
                        // 延迟触发拖拽
                        handler.postDelayed(dragRunnable, dragTriggerDelayMillis)
                        true // 必须返回 true 才能接收后续事件
                    }

                    MotionEvent.ACTION_UP -> {
                        // 取消拖拽 Runnable
                        handler.removeCallbacks(dragRunnable)
                        if (!isDragging) {
                            val pressDuration = System.currentTimeMillis() - downTime
                            if (pressDuration < ViewConfiguration.getTapTimeout()) {
                                // 短按 -> 点击查看大图
                                val context = itemView.context
                                val intent = Intent(context, PhotoViewActivity::class.java).apply {
                                    putExtra("file_url", post.file_url)
                                    putExtra("preview_url", post.preview_url)
                                    putExtra("transition_name", transitionName)
                                }
                                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    context as Activity, imageView, transitionName
                                )
                                context.startActivity(intent, options.toBundle())
                            } else {
                                // 短长按 -> 下载图片
                                downloadImage(view.context, post)
                            }
                        }
                        isDragging = false // 重置状态
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 判断是否超过滑动阈值，如果滑动则取消拖拽
                        val slop = ViewConfiguration.get(view.context).scaledTouchSlop
                        if (abs(event.x - startX) > slop || abs(event.y - startY) > slop) {
                            handler.removeCallbacks(dragRunnable)
                        }
                        false // 允许 ViewPager2 滑动
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(dragRunnable)
                        isDragging = false // 重置状态
                        true
                    }

                    else -> false
                }
            }
        }

        // 下载图片方法
        fun downloadImage(context: Context, post: Post) {
            val appFolder = R.string.app_name.toString()
            val extension = post.file_url.substringAfterLast('.', "jpg").lowercase()
            val fileName = "yande.re_${post.id}.$extension".replace("[^a-zA-Z0-9._-]".toRegex(), "_")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 及以上使用 MediaStore API
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, when (extension) {
                                "png" -> "image/png"
                                "gif" -> "image/gif"
                                else -> "image/jpeg"
                            })
                            put(MediaStore.Downloads.RELATIVE_PATH, "Download/$appFolder")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                URL(post.file_url).openStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Download completed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // 启动拖拽方法
        @OptIn(ExperimentalCoilApi::class)
        private fun startDrag(view: View, post: Post) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) // 震动反馈
            val context = view.context
            val imageLoader = context.imageLoader
            val diskCache = imageLoader.diskCache

            // 从缓存中获取图片文件
            val snapshot = diskCache?.openSnapshot(post.file_url) ?: run {
                Toast.makeText(context, R.string.image_not_cached, Toast.LENGTH_SHORT).show()
                return
            }

            val cacheFile = snapshot.data.toFile()
            val tempFile = File(context.cacheDir, "dragged_image.jpg")
            cacheFile.copyTo(tempFile, true) // 复制缓存文件到临时文件
            snapshot.close()

            // 获取临时文件的 URI
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)

            // 设置拖拽数据类型
            val mimeTypes = arrayOf("image/jpeg")
            val dragItem = ClipData.Item(uri)
            val clipData = ClipData("Image from YandeReViewer", mimeTypes, dragItem)

            val dragShadowBuilder = View.DragShadowBuilder(view)
            val dragFlags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            // 启动拖拽
            ViewCompat.startDragAndDrop(view, clipData, dragShadowBuilder, null, dragFlags)
        }
    }
}
